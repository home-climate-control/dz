package net.sf.dz3r.scheduler.gcal.v3;

import net.sf.dz3r.model.ZoneSettings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
class SettingsParserTest {

    private final Logger logger = LogManager.getLogger(getClass());

    @ParameterizedTest
    @MethodSource("testStream")
    void testAllGood(TestPair pair) {

        SettingsParser p = new SettingsParser();

        ZoneSettings settings = p.parse(pair.source);

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

            SettingsParser p = new SettingsParser();

            for (int offset = 0; offset < malformedSetpoints1.length; offset++) {

                ThreadContext.push("[" + offset + "]");
                String setpoint = malformedSetpoints1[offset];

                try {

                    assertThatIllegalArgumentException()
                            .isThrownBy(() -> p.parse(setpoint))
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
                .isThrownBy(() -> new SettingsParser().parse("off"))
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
}
