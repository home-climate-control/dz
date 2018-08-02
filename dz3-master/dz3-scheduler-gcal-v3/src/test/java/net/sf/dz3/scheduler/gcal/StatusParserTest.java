package net.sf.dz3.scheduler.gcal;

import net.sf.dz3.device.model.ZoneStatus;
import net.sf.dz3.device.model.impl.ZoneStatusImpl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import junit.framework.TestCase;

/**
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2018
 */
public class StatusParserTest extends TestCase {

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
    
    public void testAllGood() {
        
        StatusParser p = new StatusParser();
        
        for (int offset = 0; offset < inputs.length; offset++) {
            
            ThreadContext.push("[" + offset + "]");
            
            try {

                ZoneStatus status = p.parse(inputs[offset]);

                logger.info("Status: " + status);
                
                assertEquals("Failed to parse '" + inputs[offset], outputs[offset], status);
                
            } finally {
                ThreadContext.pop();
            }
        }
    }
    
    public void testCut() {
        
        String source = "2010-02-04T14:59:00.000-07:00";
        String substring = source.substring(11, 16);
        logger.info("Substring: '" + substring + "'");
        
        assertEquals("Wrong substring", "14:59", substring);
    }

    private final String[] malformedSetpoints1 = {
            "setpoint18",
            "setpoint",
    };

    public void testBadSetpoints() {

        ThreadContext.push("testBadSetpoints");

        try {

            StatusParser p = new StatusParser();

            for (int offset = 0; offset < malformedSetpoints1.length; offset++) {

                ThreadContext.push("[" + offset + "]");
                String setpoint = malformedSetpoints1[offset];

                try {

                    ZoneStatus status = p.parse(setpoint);

                    fail("should've blown up by now");

                } catch (IllegalArgumentException ex) {

                    // This is expected
                    logger.debug("Expected exception", ex);

                    assertEquals("wrong exception message",
                            "can't parse '" + setpoint + "' (malformed setpoint '" + setpoint + "')",
                            ex.getMessage());

                } catch (Throwable t) {

                    logger.error("Oops", t);
                    fail("wrong exception thrown, see the log");

                } finally {
                    ThreadContext.pop();
                }
            }
        } finally {
            ThreadContext.pop();
        }
    }
}
