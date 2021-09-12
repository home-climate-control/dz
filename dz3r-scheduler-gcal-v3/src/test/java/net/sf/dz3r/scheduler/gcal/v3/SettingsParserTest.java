package net.sf.dz3r.scheduler.gcal.v3;

import net.sf.dz3r.model.ZoneSettings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
class SettingsParserTest {

    private final Logger logger = LogManager.getLogger(getClass());

    private final String[] inputs = {
            "setpoint 18",
            "setpoint 18C, enabled, voting",
            "setpoint 18C, on, voting, dump priority = 2",
            "setpoint 18C, enabled, voting, dump priority: 2",
            "setpoint = 18C; enabled; voting; dump priority 2",
            "enabled; not voting; setpoint = 80F",
            "disabled; non-voting, setpoint: 80F",
            "off; non-voting, setpoint: 80F"
    };

    private final ZoneSettings[] outputs = {
            new ZoneSettings(true, 18.0, true, false, 0),
            new ZoneSettings(true, 18.0, true, false, 0),
            new ZoneSettings(true, 18.0, true, false, 2),
            new ZoneSettings(true, 18.0, true, false, 2),
            new ZoneSettings(true, 18.0, true, false, 2),
            new ZoneSettings(true, 26.666666666666668, false, false, 0),
            new ZoneSettings(false, 26.666666666666668, false, false, 0),
            new ZoneSettings(false, 26.666666666666668, false, false, 0),
    };

    @Test
    void testAllGood() {

        SettingsParser p = new SettingsParser();

        for (int offset = 0; offset < inputs.length; offset++) {

            ThreadContext.push("[" + offset + "]");

            try {

                ZoneSettings settings = p.parse(inputs[offset]);

                logger.info("Command:  {}", inputs[offset]);
                logger.info("Settings: {}", settings);

                assertThat(settings).isEqualTo(outputs[offset]);

            } finally {
                ThreadContext.pop();
            }
        }
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
}
