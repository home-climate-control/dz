package net.sf.dz3.scheduler.gcal;

import net.sf.dz3.device.model.ZoneStatus;
import net.sf.dz3.device.model.impl.ZoneStatusImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * 
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2018
 */
class StatusParserTest {

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
    
    private final ZoneStatus[] outputs = {
            new ZoneStatusImpl(18, 0, true, true),
            new ZoneStatusImpl(18, 0, true, true),
            new ZoneStatusImpl(18, 2, true, true),
            new ZoneStatusImpl(18, 2, true, true),
            new ZoneStatusImpl(18, 2, true, true),
            new ZoneStatusImpl(26.666666666666668, 0, true, false),
            new ZoneStatusImpl(26.666666666666668, 0, false, false),
            new ZoneStatusImpl(26.666666666666668, 0, false, false),
    };

    @Test
    public void testAllGood() {
        
        StatusParser p = new StatusParser();
        
        for (int offset = 0; offset < inputs.length; offset++) {
            
            ThreadContext.push("[" + offset + "]");
            
            try {

                ZoneStatus status = p.parse(inputs[offset]);

                logger.info("Status: " + status);

                assertThat(status).as("Failed to parse '" + inputs[offset]).isEqualTo(outputs[offset]);

            } finally {
                ThreadContext.pop();
            }
        }
    }

    @Test
    public void testCut() {
        
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
    public void testBadSetpoints() {

        ThreadContext.push("testBadSetpoints");

        try {

            StatusParser p = new StatusParser();

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
    public void testOff() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new StatusParser().parse("off"))
                .withMessage("Could not parse setpoint out of 'off'");
    }
}
