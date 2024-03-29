package net.sf.dz3r.scheduler.gcal.v3;

import net.sf.dz3r.model.ZoneSettings;
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

    public ZoneSettings parse(String arguments) {

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

            return new ZoneSettings(
                    Optional.ofNullable(enabled).orElse(true),
                    setpoint,
                    Optional.ofNullable(voting).orElse(true),
                    null,
                    Optional.ofNullable(dumpPriority).orElse(0));

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
}
