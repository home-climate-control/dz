package net.sf.dz3.runtime.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.sf.dz3.runtime.config.schedule.CalendarConfigEntry;
import net.sf.dz3.runtime.config.schedule.ScheduleConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;

class HccRawConfigTest {

    private final Logger logger = LogManager.getLogger();

    /**
     * @return Object mapper configured the same way as it is in {@code ApplicationBase}.
     */
    private ObjectMapper getMapper() {

        var objectMapper = new ObjectMapper(new YAMLFactory());

        // Necessary to print Optionals in a sane way
        objectMapper.registerModule(new Jdk8Module());

        // Necessary to deal with Duration
        objectMapper.registerModule(new JavaTimeModule());

        // For Quarkus to deal with interfaces nicer
        objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

        // For standalone to allow to ignore the root element
        // VT: NOTE: Not necessary here
        // objectMapper.enable(DeserializationFeature.UNWRAP_ROOT_VALUE);

        return objectMapper;
    }

    /**
     * Confirm the correct syntax for {@link ScheduleConfig} - Spring and Quarkus parsers are more permissive than standalone.
     */
    @Test
    void scheduleYamlSerialization() throws IOException {

        var scheduleConfig = new ScheduleConfig(
                Set.of(
                        new CalendarConfigEntry("a", "1"),
                        new CalendarConfigEntry("b", "2")
                ));
        var config = new HccRawConfig(
                null,
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                scheduleConfig,
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                null,
                null);

        var objectMapper = getMapper();

        var result = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(config);

        // Hmm... Quarkus and Spring will take the ScheduleConfig YAML even if the zone/calendar pairs are shifted
        // one tab to the right
        logger.debug("YAML:\n{}", result);

        assertThatCode(() -> {
            objectMapper.readValue(new StringReader(result), HccRawConfig.class);
        })
                .doesNotThrowAnyException();
    }
}
