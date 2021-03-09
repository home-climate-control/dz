package net.sf.dz3.scheduler.gcal;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import net.sf.dz3.device.model.ZoneStatus;
import net.sf.dz3.device.model.impl.ZoneStatusImpl;
import junit.framework.TestCase;

/**
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2010
 */
public class StatusParserTest extends TestCase {

    private final Logger logger = Logger.getLogger(getClass());

    private final String[] inputs = {
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
            
            NDC.push("[" + offset + "]");
            
            try {

                ZoneStatus status = p.parse(inputs[offset]);

                logger.info("Status: " + status);
                
                assertEquals("Failed to parse '" + inputs[offset], outputs[offset], status);
                
            } finally {
                NDC.pop();
            }
        }
    }
    
    public void testCut() {
        
        String source = "2010-02-04T14:59:00.000-07:00";
        String substring = source.substring(11, 16);
        logger.info("Substring: '" + substring + "'");
        
        assertEquals("Wrong substring", "14:59", substring);
    }
}
