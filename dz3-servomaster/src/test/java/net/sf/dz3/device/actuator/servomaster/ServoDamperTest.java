package net.sf.dz3.device.actuator.servomaster;

import com.homeclimatecontrol.jukebox.sem.ACT;
import net.sf.dz3.device.actuator.Damper;
import net.sf.servomaster.device.impl.debug.NullServoController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.within;

class ServoDamperTest {

    private final Logger logger = LogManager.getLogger(getClass());

    @Test
    void parkDefault() throws IOException {

        var controller = new NullServoController();
        controller.open();
        Damper d = new ServoDamper("sd", controller.getServo("0"));

        assertThatCode(() -> {
            // Parking position hasn't been explicitly set
            d.park();
        }).doesNotThrowAnyException();

        assertThat(d.getPosition()).isEqualTo(d.getParkPosition());
    }

    /**
     * Test whether the {@link ServoDamper} is properly parked.
     */
    @Test
    void park() throws IOException {

        var controller = new NullServoController();
        controller.open();
        Damper d = new ServoDamper("sd", controller.getServo("0"));

        assertThatCode(() -> {

            // Test with default parked position first
            testPark(d, null);

            for (double parkedPosition = 0.9; parkedPosition > 0.05; parkedPosition -= 0.1) {

                testPark(d, parkedPosition);
            }
        }).doesNotThrowAnyException();
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
            assertThat(target.getParkPosition()).as("parked position").isEqualTo(parkedPosition, within(0.001));
        }

        logger.info("park position: " + parkedPosition);

        target.set(0);
        target.set(1);
        target.set(0);

        ACT parked = target.park();

        if (!parked.waitFor()) {
            fail("Failed to park, see the log");
        }

        assertThat(target.getPosition()).as("parked position").isEqualTo(target.getParkPosition(), within(0.001));
    }
}
