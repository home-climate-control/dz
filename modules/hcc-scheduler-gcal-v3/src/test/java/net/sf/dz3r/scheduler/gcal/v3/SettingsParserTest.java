package net.sf.dz3r.scheduler.gcal.v3;

import com.google.api.services.calendar.model.Event;
import com.homeclimatecontrol.hcc.model.ZoneSettings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2024
 */
class SettingsParserTest {

    private final Logger logger = LogManager.getLogger(getClass());
    private final SettingsParser parser = new SettingsParser();

    @ParameterizedTest
    @MethodSource("testStream")
    @Disabled("Overriding equals() makes the code fragile, have to find and weed out all occurrences")
    void testAllGood(TestPair pair) {

        var settings = parser.parseSettings(pair.source);

        logger.info("Command:  {}", pair.source);
        logger.info("Settings: {}", settings);

        assertThat(settings).isEqualTo(pair.result);
    }

    @Test
    void testCut() {

        String source = "2010-02-04T14:59:00.000-07:00";
        String substring = source.substring(11, 16);
        logger.info("Substring: '" + substring + "'");

        assertThat(substring).isEqualTo("14:59");
    }

    private final String[] malformedSetpoints1 = {
            "setpoint18",
            "setpoint",
    };

    @Test
    void testBadSetpoints() {

        ThreadContext.push("testBadSetpoints");

        try {

            for (int offset = 0; offset < malformedSetpoints1.length; offset++) {

                ThreadContext.push("[" + offset + "]");
                String setpoint = malformedSetpoints1[offset];

                try {

                    assertThatIllegalArgumentException()
                            .isThrownBy(() -> parser.parseSettings(setpoint))
                            .withMessage("can't parse '" + setpoint + "' (malformed setpoint '" + setpoint + "')");

                } finally {
                    ThreadContext.pop();
                }
            }

        } finally {
            ThreadContext.pop();
        }
    }

    @Test
    void testOff() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> parser.parseSettings("off"))
                .withMessage("Could not parse setpoint out of 'off'");
    }

    private record TestPair(
        String source,
        ZoneSettings result
    ) {}

    private static Stream<TestPair> testStream() {
        return Stream.of(
                new TestPair(
                        "setpoint 18",
                        new ZoneSettings(true, 18.0, true, null, 0, null)
                ),
                new TestPair(
                        "setpoint 18C, enabled, voting",
                        new ZoneSettings(true, 18.0, true, null, 0, null)
                ),
                new TestPair(
                        "setpoint 18C, on, voting, dump priority = 2",
                        new ZoneSettings(true, 18.0, true, null, 2, null)
                ),
                new TestPair(
                        "setpoint 18C, enabled, voting, dump priority: 2",
                        new ZoneSettings(true, 18.0, true, null, 2, null)
                ),
                new TestPair(
                        "setpoint = 18C; enabled; voting; dump priority 2",
                        new ZoneSettings(true, 18.0, true, null, 2, null)
                ),
                new TestPair(
                        "enabled; not voting; setpoint = 80F",
                        new ZoneSettings(true, 26.666666666666668, false, null, 0, null)
                ),
                new TestPair(
                        "disabled; non-voting, setpoint: 80F",
                        new ZoneSettings(false, 26.666666666666668, false, null, 0, null)
                ),
                new TestPair(
                        "off; non-voting, setpoint: 80F",
                        new ZoneSettings(false, 26.666666666666668, false, null, 0, null)
                )
        );
    }

    @ParameterizedTest
    @MethodSource("names")
    void parsePeriodName(NameSource source) {
        var e = new Event();

        e.setSummary(source.summary);
        e.setDescription(source.description);

        if (source.exceptionMessage == null) {

            assertThat(parser.parsePeriodName(e)).isEqualTo(source.expected);

        } else {

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> parser.parsePeriodName(e))
                    .withMessage(source.exceptionMessage);
        }
    }

    static record NameSource(
            String summary,
            String description,
            String expected,
            String exceptionMessage
    ) {

    }
    public static Stream<NameSource> names() {

        return Stream.of(
                // The presence of the description here overrides the colon in the summary
                new NameSource(
                        "Evening: everyone is at home # 28.6C",
                        """
                            setpoint: 28.6
                            economizer:
                              changeover-delta: 2
                              target-temperature: 27
                              keep-hvac-on: false""",
                        "Evening: everyone is at home",
                        null
                ),
                new NameSource(
                        "Sleep: setpoint 18c",
                        null,
                        "Sleep",
                        null
                ),
                new NameSource(
                        "Sleep: setpoint 18c",
                        "",
                        "Sleep",
                        null
                ),
                new NameSource(
                        "Sleep setpoint 18c",
                        null,
                        null,
                        "Can't parse period name out of event title 'Sleep setpoint 18c' (must be separated by a colon, and not empty)"
                ),
                new NameSource(
                        ":setpoint 18c",
                        null,
                        null,
                        "Can't parse period name out of event title ':setpoint 18c' (must be separated by a colon, and not empty)"
                ),
                new NameSource(
                        "Sleep # 28.6C",
                        """
                            setpoint: 28.6
                            economizer:
                              changeover-delta: 2
                              target-temperature: 27
                              keep-hvac-on: false""",
                        "Sleep",
                        null
                ),
                new NameSource(
                        "# Sleep at 28.6C",
                        """
                            setpoint: 28.6
                            economizer:
                              changeover-delta: 2
                              target-temperature: 27
                              keep-hvac-on: false""",
                        null,
                        "Can't parse period name out of event title '# Sleep at 28.6C' (empty text before '#')"
                )
        );
    }
}
