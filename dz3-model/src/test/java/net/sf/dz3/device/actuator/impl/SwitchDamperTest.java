package net.sf.dz3.device.actuator.impl;

import com.homeclimatecontrol.jukebox.sem.ACT;
import net.sf.dz3.device.actuator.Damper;
import net.sf.dz3.device.sensor.impl.NullSwitch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Test case for {@link SwitchDamper}.
 * 
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2018
 */
class SwitchDamperTest {

    private final Logger logger = LogManager.getLogger(getClass());
    
    /**
     * Test whether the {@link SwitchDamper} is properly parked.
     */
    @Test
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
    private void testPark(Damper target, Double parkedPosition) throws IOException, InterruptedException {
        
        if (parkedPosition != null) {
            
            target.setParkPosition(parkedPosition);
            assertThat(target.getParkPosition()).as("parked position").isEqualTo(parkedPosition);
        }
        
        logger.info("park position: " + parkedPosition);
        
        target.set(0);
        target.set(1);
        target.set(0);
        
        ACT parked = target.park();
        
        if (!parked.waitFor()) {
            fail("Failed to park, see the log");
        }

        assertThat(target.getPosition()).as("parked position").isEqualTo(target.getParkPosition());
    }
}
