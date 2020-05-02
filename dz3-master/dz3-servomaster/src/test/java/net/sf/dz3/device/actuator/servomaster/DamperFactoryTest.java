package net.sf.dz3.device.actuator.servomaster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.junit.Test;

import net.sf.servomaster.device.impl.debug.NullServoController;

public class DamperFactoryTest {

    private final Logger logger = LogManager.getLogger(getClass());

    @Test
    public void badClass() {

        ThreadContext.push("badClass");
        try {

            new DamperFactory("badClass", null);
            fail("Should've thrown an exception already");

        } catch (IllegalArgumentException ex) {

            logger.debug("We're fine", ex);
            assertTrue("Wrong exception message", ex.getMessage().startsWith("can't instantitate"));

        } finally {
            ThreadContext.pop();
        }
    }

    @Test
    public void notController() {

        ThreadContext.push("notController");
        try {

            new DamperFactory("java.lang.String", null);
            fail("Should've thrown an exception already");

        } catch (IllegalArgumentException ex) {

            logger.debug("We're fine", ex);
            assertEquals("Wrong exception message", "Not a servo controller: java.lang.String", ex.getMessage());

        } finally {
            ThreadContext.pop();
        }
    }

    @Test
    public void instantiate() {

        ThreadContext.push("notController");
        try {

            new DamperFactory(NullServoController.class.getName(), "port");
            assertTrue(true);

        } finally {
            ThreadContext.pop();
        }
    }
}
