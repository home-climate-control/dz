package com.homeclimatecontrol.hcc.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class ZoneSettingsTest {

    private final Logger logger = LogManager.getLogger();

    @Test
    void testJSON() {
        assertThatCode(() -> testOutput(new ObjectMapper(), "JSON")).doesNotThrowAnyException();
    }

    @Test
    void testYaml() {
        assertThatCode(() -> testOutput(new ObjectMapper(new YAMLFactory()), "YAML")).doesNotThrowAnyException();
    }

    private void testOutput(ObjectMapper objectMapper, String marker) throws JsonProcessingException {

        var source = new ZoneSettings(
                true,
                25.0,
                true,
                false,
                2,
                new EconomizerSettings(2, 22.0, null, 0.5)
        );
        var result = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(source);

        logger.debug("{}:\n{}", marker, result);
    }

    @ParameterizedTest
    @MethodSource("settingsProvider")
    void same(Source source) {
        assertThat(source.a.same(source.b)).isEqualTo(source.same);
    }

    private record Source(
            ZoneSettings a,
            ZoneSettings b,
            boolean same
    ) {

    }

    private static Stream<Source> settingsProvider() {

        return Stream.of(
                new Source(
                        new ZoneSettings(25.0),
                        new ZoneSettings(25.0),
                        true
                ),
                new Source(
                        new ZoneSettings(
                                true,
                                25d,
                                true,
                                false,
                                0,
                                null),
                        new ZoneSettings(
                                null,
                                25d,
                                null,
                                null,
                                null,
                                null),
                        true
                ),
                new Source(
                        new ZoneSettings(
                                true,
                                25d,
                                true,
                                false,
                                0,
                                null),
                        new ZoneSettings(
                                null,
                                25d,
                                null,
                                null,
                                null,
                                new EconomizerSettings(
                                        2,
                                        22,
                                        null,
                                        null
                                )
                        ),
                        false
                ),
                new Source(
                        new ZoneSettings(
                                true,
                                25d,
                                true,
                                false,
                                0,
                                new EconomizerSettings(
                                        2d,
                                        22d,
                                        null,
                                        null
                                )
                        ),
                        new ZoneSettings(
                                null,
                                25d,
                                null,
                                null,
                                null,
                                new EconomizerSettings(
                                        2d,
                                        22d,
                                        true,
                                        1d
                                )
                        ),
                        true
                )
        );
    }
}
