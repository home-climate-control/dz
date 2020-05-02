package net.sf.dz3.device.actuator.servomaster;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import junit.framework.TestCase;

public class DamperFactoryTest extends TestCase {

    private final Logger logger = LogManager.getLogger(getClass());

    public void testBadClassName() {

        try {

            new DamperFactory("badClass", null);
            fail("Should've thrown an exception already");

        } catch (IllegalArgumentException ex) {
            logger.debug("We're fine", ex);
            assertTrue("Wrong exception message", ex.getMessage().startsWith("can't instantitate"));
        }
    }

    public void testNotController() {

        try {

            new DamperFactory("java.lang.String", null);
            fail("Should've thrown an exception already");

        } catch (IllegalArgumentException ex) {
            logger.debug("We're fine", ex);
            assertTrue("Wrong exception message", ex.getMessage().startsWith("Not a servo controller"));
        }
    }

}
