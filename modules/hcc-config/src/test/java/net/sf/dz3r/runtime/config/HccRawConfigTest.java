package net.sf.dz3r.runtime.config;

import net.sf.dz3r.runtime.config.schedule.CalendarConfigEntry;
import net.sf.dz3r.runtime.config.schedule.ScheduleConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.IOException;
import java.io.StringReader;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;

class HccRawConfigTest {

    private final Logger logger = LogManager.getLogger();

    /**
     * @return Object mapper configured the same way as it is in {@code ApplicationBase}.
     */
    private YAMLMapper getMapper() {

        return YAMLMapper
                .builder()
                // For Quarkus to deal with interfaces nicer
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
                // For standalone to allow to ignore the root element
                // VT: NOTE: Not necessary here
                // .enable(DeserializationFeature.UNWRAP_ROOT_VALUE)
                .build();
    }

    /**
     * Confirm the correct syntax for {@link ScheduleConfig} - Spring parser is more permissive than standalone.
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
                null,
                Set.of(),
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
                null,
                null);

        var yamlMapper = getMapper();

        var result = yamlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(config);

        // Hmm... Spring will take the ScheduleConfig YAML even if the zone/calendar pairs are shifted one tab to the right
        logger.debug("YAML:\n{}", result);

        assertThatCode(() -> {
            yamlMapper.readValue(new StringReader(result), HccRawConfig.class);
        })
                .doesNotThrowAnyException();
    }
}
