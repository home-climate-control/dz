package net.sf.dz3r.scheduler.gcal.v3;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import net.sf.dz3r.scheduler.gcal.v3.SettingsParser.ZoneSettingsYaml.EconomizerSettingsYaml;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Flux;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class SettingsParserYamlTest {

    private final Logger logger = LogManager.getLogger(getClass());

    private static final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

    private final SettingsParser parser = new SettingsParser();

    /**
     * See how YAML is rendered from {@link SettingsParser.ZoneSettingsYaml}.
     */
    @Test
    void render() throws JsonProcessingException {

        var source = Flux.just(
                new SettingsParser.ZoneSettingsYaml(
                        null,
                        22d,
                        null,
                        null,
                        new EconomizerSettingsYaml(2d, 20d, true, 1.0)
                ),
                new SettingsParser.ZoneSettingsYaml(
                        null,
                        23d,
                        null,
                        null,
                        null
                )
        );

        source
                .doOnNext(s -> {

                    assertThatCode(() -> {
                        var yaml = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(s);

                        logger.info("YAML event settings parsed:\n{}", yaml);
                    }).doesNotThrowAnyException();
                })
                .blockLast();

    }

    /**
     * Make sure that well formed YAML is parsed correctly.
     */
    @ParameterizedTest
    @MethodSource("validSettings")
    void parseRaw(String source) {

        assertThatCode(() -> {

            assertThat(parser.parseAsYaml(source)).isNotNull();

        }).doesNotThrowAnyException();
    }

    /**
     * Make sure that YAML that contains non-breaking space fails the way we think it will.
     */
    @ParameterizedTest
    @MethodSource("googleCalendarSettings")
    void parseGoogleFail(String source) {

        assertThatExceptionOfType(UnrecognizedPropertyException.class)
                .isThrownBy(() -> parser.parseAsYaml(source))
                .withMessageStartingWith("Unrecognized field \"\u00A0 changeover-delta\"");
    }

    /**
     * Make sure that what we do to correct the problem does indeed fix it.
     */
    @ParameterizedTest
    @MethodSource("googleCalendarSettings")
    void parseGoogleReplaced(String source) {

        assertThatCode(() -> {

            assertThat(parser.parseAsYaml(source.replace('\u00A0',' '))).isNotNull();

        }).doesNotThrowAnyException();
    }

    public static Stream<String> validSettings() {
        return Stream.of(
                """
                        setpoint: 25
                        """,
                """
                        setpoint: 23
                        economizer:
                          changeover-delta: 2
                          target-temperature: 20
                          max-power: 0.3
                        """,
                """
                        setpoint: 28.6
                        economizer:
                          changeover-delta: 2
                          target-temperature: 27
                          keep-hvac-on: false"""
        );
    }

    public static Stream<String> googleCalendarSettings() {
        return Stream.of(
                """
                        setpoint: 28.9
                        economizer:
                        \u00A0 changeover-delta: 3
                        \u00A0 target-temperature: 25
                        \u00A0 keep-hvac-on: false"""

        );
    }
}
