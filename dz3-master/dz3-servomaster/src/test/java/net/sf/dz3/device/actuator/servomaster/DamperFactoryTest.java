package net.sf.dz3.device.actuator.servomaster;

import junit.framework.TestCase;

import org.apache.log4j.Logger;

public class DamperFactoryTest extends TestCase {
    
    private final Logger logger = Logger.getLogger(getClass());
    
    public void testBadClassName() {
        
        try {

            @SuppressWarnings("unused")
            DamperFactory df = new DamperFactory("badClass", null);
            fail("Should've thrown an exception already");
            
        } catch (IllegalArgumentException ex) {
            logger.debug("We're fine", ex);
            assertTrue("Wrong exception message", ex.getMessage().startsWith("Can't find class for name"));
        }
    }

    public void testNotController() {
        
        try {

            @SuppressWarnings("unused")
            DamperFactory df = new DamperFactory("java.lang.String", null);
            fail("Should've thrown an exception already");
            
        } catch (IllegalArgumentException ex) {
            logger.debug("We're fine", ex);
            assertTrue("Wrong exception message", ex.getMessage().startsWith("Not a servo controller"));
        }
    }

}
