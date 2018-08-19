package net.sf.dz3.device.actuator.impl;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import junit.framework.TestCase;
import net.sf.dz3.device.actuator.Damper;
import net.sf.dz3.device.sensor.impl.NullSwitch;
import net.sf.servomaster.device.model.TransitionStatus;

/**
 * Test case for {@link SwitchDamper}.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko</a> 2001-2018
 */
public class SwitchDamperTest extends TestCase {

    private final Logger logger = LogManager.getLogger(getClass());
    
    /**
     * Test whether the {@link SwitchDamper} is properly parked.
     */
    public void testPark() {
        
        NullSwitch s = new NullSwitch("switch");
        Damper d = new SwitchDamper("damper", s, 0.5);
        
        try {

            // Test with default parked position first
            testPark(d, null);

            for (double parkedPosition = 0.9; parkedPosition > 0.05; parkedPosition -= 0.1) {

                testPark(d, parkedPosition);
            }

        } catch (Throwable t) {
            
            logger.fatal("Oops", t);
            fail(t.getMessage());
        }
    }

    
    /**
     * Test if the damper is properly parked in a given position. 
     * 
     * @param target Damper to test.
     * @param parkedPosition Position to park in.
     */
    private void testPark(Damper target, Double parkedPosition) throws IOException, InterruptedException, ExecutionException {
        
        if (parkedPosition != null) {
            
            target.setParkPosition(parkedPosition);
            assertEquals("Couldn't properly set parked position", parkedPosition, target.getParkPosition());
        }
        
        logger.info("park position: " + parkedPosition);
        
        target.set(0);
        target.set(1);
        target.set(0);
        
        Future<TransitionStatus> parked = target.park();

        assertNotNull("Future is null", parked);

        TransitionStatus s = parked.get();

        if (!s.isOK()) {
            fail("Failed to park, see the log");
        }
        
        assertEquals("Wrong parked position", target.getParkPosition(), target.getPosition());
    }

    /**
     * Make sure non-inverted switch works as expected.
     */
    public void testPositionStraight() throws InterruptedException, ExecutionException, IOException {

        ThreadContext.push("testPositionStraight");

        try {

            NullSwitch s = new NullSwitch("switch");

            assertEquals("wrong switch state", false, s.getState());

            Damper d = new SwitchDamper("damper", s, 0.5);

            assertEquals("wrong switch state", true, s.getState());

            d.set(0).get();

            assertEquals("wrong switch state", false, s.getState());

            d.set(1).get();

            assertEquals("wrong switch state", true, s.getState());

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Make sure inverted switch works as expected.
     */
    public void testPositionInverted() throws InterruptedException, ExecutionException, IOException {

        ThreadContext.push("testPositionInverted");

        try {

            NullSwitch s = new NullSwitch("switch");

            assertEquals("wrong switch state", false, s.getState());

            s.setState(true);

            assertEquals("wrong switch state", true, s.getState());

            Damper d = new SwitchDamper("damper", s, 0.5, 1.0, true);

            assertEquals("wrong switch state", false, s.getState());


            d.set(0).get();

            assertEquals("wrong switch state", true, s.getState());

            d.set(1).get();

            assertEquals("wrong switch state", false, s.getState());

        } finally {
            ThreadContext.pop();
        }
    }
}
