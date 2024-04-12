package net.sf.dz3r.scheduler.gcal.v3;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.api.services.calendar.model.Event;
import net.sf.dz3r.device.actuator.economizer.EconomizerSettings;
import net.sf.dz3r.model.ZoneSettings;
import net.sf.dz3r.scheduler.gcal.v3.SettingsParser.ZoneSettingsYaml.EconomizerSettingsYaml;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.StringTokenizer;

/**
 * Utility class to parse the {@link ZoneSettings} out of a period free form text.
 *
 * Eventually, this utility can become very smart in parsing badly formed free form input. However,
 * this will also cause it to bloat quite a bit and become too heavy. It is possible to offload
 * parsing on an online component, but whether this is necessary is yet to be seen.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class SettingsParser {

    private final Logger logger = LogManager.getLogger();
    private final NumberFormat numberFormat = NumberFormat.getInstance();

    private ObjectMapper objectMapper;

    private synchronized ObjectMapper getMapper() {

        if (objectMapper == null) {
            objectMapper = new ObjectMapper(new YAMLFactory());
        }

        return objectMapper;
    }

    /**
     * Parse period name, two ways.
     *
     * Legacy: if there's a colon, the period name is everything before it. This will go away along with parsing settings out of the event summary.
     * Going forward: if the event description is not empty, treat the whole summary before "#" as a period name.
     *
     * @param source Event to parse the period name from.
     *
     * @return Period name.
     */
    public String parsePeriodName(Event source) {

        var summary = source.getSummary();
        var description = source.getDescription();

        if (description == null || description.isEmpty()) {
            return parseLegacyPeriodName(summary);
        }

        var name = StringUtils.substringBefore(summary, '#').trim();

        if (name.isEmpty()) {
            throw new IllegalArgumentException("Can't parse period name out of event title '" + summary + "' (empty text before '#')");
        }

        return name;
    }

    private String parseLegacyPeriodName(String summary) {
        int colonIndex = summary.indexOf(':');

        if (colonIndex <= 0) {
            throw new IllegalArgumentException("Can't parse period name out of event title '" + summary + "' (must be separated by a colon, and not empty)");
        }

        return summary.substring(0, colonIndex).trim();
    }

    /**
     * Parse the complete zone settings (with economizer settings) from the whole event and if it doesn't work,
     * try to parse it {@link #parseSettings(String) the old style}.
     *
     * @param source Event to parse.
     * @param settingsAsString Legacy argument - the summary substring; will go away.
     */
    public ZoneSettings parseSettings(Event source, String settingsAsString) {

        return Optional
                .ofNullable(parseSettings(source))
                .orElseGet(() -> parseSettings(settingsAsString));
    }

    /**
     * Parse just the event.
     * @param source Event to parse.
     *
     * @return Parsed settings, or {@code null} if settings couldn't have been parsed this way.
     */
    private ZoneSettings parseSettings(Event source) {

        var summary = source.getSummary();
        var description = source.getDescription();

        if (description == null || description.isEmpty()) {
            logger.debug("Missing description for event '{}', reverting to old syntax", summary);
            return null;
        }

        ThreadContext.push("parseAsYaml");
        try {

            // VT: Two fucking hours of my life on catching this &nbsp; nobody ever asked for.
            return parseAsYaml(description.replace('\u00A0',' '));

        } catch (JsonProcessingException ex) {
            logger.error("Can't parse '{}' body as YAML, reverting to old syntax:\n{}", summary, source, ex);
            return null;
        } finally {
            ThreadContext.pop();
        }
    }

    ZoneSettings parseAsYaml(String source) throws JsonProcessingException {

            var result = getMapper()
                    .readerFor(ZoneSettingsYaml.class)
                    .withoutRootName()
                    .readValue(source);
            var yaml = getMapper().writerWithDefaultPrettyPrinter().writeValueAsString(result);

            logger.debug("YAML event settings parsed:\n{}", yaml);

            return convert((ZoneSettingsYaml) result);
    }

    private ZoneSettings convert(ZoneSettingsYaml source) {

        return new ZoneSettings(
                source.enabled,
                source.setpoint,
                source.voting,
                null,
                source.dumpPriority,
                convert(source.economizer)
        );
    }

    private EconomizerSettings convert(EconomizerSettingsYaml source) {

        if (source == null) {
            return null;
        }

        return new EconomizerSettings(
                source.changeoverDelta,
                source.targetTemperature,
                source.keepHvacOn,
                source.maxPower
        );
    }

    /**
     * Parse the old zone settings without economizer settings from just the event summary.
     *
     * @deprecated This syntax will go away soon, use the new syntax instead.
     */
    ZoneSettings parseSettings(String arguments) {

        ThreadContext.push("parse");

        arguments = arguments.toLowerCase().trim();

        Double setpoint = null;
        Boolean enabled = null;
        Boolean voting = null;
        Integer dumpPriority = null;

        try {

            for (StringTokenizer st = new StringTokenizer(arguments, ",;"); st.hasMoreTokens(); ) {

                String token = st.nextToken().trim();

                logger.trace("Token: '{}'", token);

                switch (token) {
                    case "on", "enabled" -> enabled = true;
                    case "off", "disabled" -> enabled = false;
                    case "voting" -> voting = true;
                    case "non-voting", "not voting" -> voting = false;
                    default -> {
                        setpoint = setpoint != null ? setpoint : tryParseSetpoint(token, arguments);
                        dumpPriority = tryParseDumpPriority(token);
                    }
                }
            }

            // There is no default for setpoint, the rest of them are in the constructor invocation below
            if (setpoint == null) {
                throw new IllegalArgumentException("Could not parse setpoint out of '" + arguments + "'");
            }

            var result = new ZoneSettings(
                    enabled,
                    setpoint,
                    voting,
                    null,
                    dumpPriority);

            // Let's make the transition easier and pretty print the parsed settings

            var yaml = getMapper().writerWithDefaultPrettyPrinter().writeValueAsString(result);
            logger.debug("Settings as YAML:\n{}", yaml);

            return result;

        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Successfully parsed '" + arguments + "' but couldn't pretty print them", ex);
        } finally {
            ThreadContext.pop();
        }
    }

    private Double tryParseSetpoint(String token, String arguments) {

        if (!token.startsWith("setpoint") && !token.startsWith("temperature")) {
            return null;
        }

        StringTokenizer st2 = new StringTokenizer(token, " =:");

        // Result is not needed
        st2.nextToken();

        try {
            return parseSetpoint(st2.nextToken());
        } catch (NoSuchElementException ex) {

            // This indicates a problem with setpoint syntax
            throw new IllegalArgumentException("can't parse '" + arguments + "' (malformed setpoint '" + token + "')", ex);
        }
    }

    private Double parseSetpoint(String setpoint) {

        try {

            // From the docs:
            //
            // Number formats are generally not synchronized.
            // It is recommended to create separate format instances for each thread.
            // If multiple threads access a format concurrently, it must be synchronized
            // externally.

            synchronized (numberFormat) {

                var n = numberFormat.parse(setpoint);
                var value = n.doubleValue();

                // Default temperature unit is Celsius, you'll have to explicitly specify
                // Fahrenheit if you want it

                if (setpoint.contains("f")) {
                    // Need to convert to Celsius
                    logger.trace("Temperature unit is Fahhrenheit");
                    value = ((value - 32) * 5) / 9;
                }

                return value;
            }

        } catch (ParseException|NumberFormatException ex) {
            throw new IllegalArgumentException("Could not parse setpoint out of '" + setpoint + "'", ex);
        }
    }

    private Integer tryParseDumpPriority(String dumpPriority) {

        if (!dumpPriority.startsWith("dump")) {
            return null;
        }

        // ["dumpPriority" || "dump priority"] [= : " "] <value>

        for (StringTokenizer st = new StringTokenizer(dumpPriority, " =:"); st.hasMoreTokens(); ) {

            var token = st.nextToken();

            try {

                return numberFormat.parse(token).intValue();

            } catch (ParseException ex) {

                // Well, this didn't work, let's go to next
                logger.debug("Couldn't parse '{}' as a number", token);
            }
        }

        throw new IllegalArgumentException("Could not parse dump priority out of '" + dumpPriority + "'");
    }

    /**
     * An immutable behaviorless copy of {@link net.sf.dz3r.model.ZoneSettings}.
     *
     * It's easier to have it here than to deal with Jackson shenanigans.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public record ZoneSettingsYaml(
            Boolean enabled,
            Double setpoint,
            Boolean voting,
            Integer dumpPriority,
            EconomizerSettingsYaml economizer
    ) {
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
        public record EconomizerSettingsYaml(
                Double changeoverDelta,
                Double targetTemperature,
                Boolean keepHvacOn,
                Double maxPower
        ) {

        }
    }
}
