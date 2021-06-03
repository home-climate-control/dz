package net.sf.dz3.device.actuator.impl;

import net.sf.dz3.device.model.HvacMode;
import net.sf.dz3.device.sensor.impl.NullSwitch;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class HvacDriverHeatpumpTest {

    /**
     * See <a href="https://github.com/home-climate-control/dz/issues/3">#3</a> for details.
     */
    @Test
    void runaway() throws IOException { // NOSONAR This is a complicated failure, so the test is also complicated

        var switchMode = new NullSwitch("mode");
        var switchRunning = new NullSwitch("running");
        var switchFan = new NullSwitch("fan");

        var driver = new HvacDriverHeatpump(switchMode, switchRunning, switchFan);

        {
            // Change the mode and verify that it worked

            assertThat(switchMode.getState()).as("mode before the switch").isFalse();
            driver.setMode(HvacMode.HEATING);
            assertThat(switchMode.getState()).as("mode after the switch").isTrue();
        }

        {
            // Normal cycle

            driver.setFanSpeed(1);
            driver.setStage(1);

            double[] fanSpeed = driver.getFanSpeed();
            int[] stage = driver.getStage();

            assertThat(fanSpeed[0]).as("expected fan speed").isEqualTo(1.0);
            assertThat(fanSpeed[1]).as("actual fan speed").isEqualTo(1.0);
            assertThat(switchFan.getState()).as("fan speed switch state").isTrue();
            assertThat(stage[0]).as("expected stage").isEqualTo(1);
            assertThat(stage[1]).as("actual stage").isEqualTo(1);
            assertThat(switchFan.getState()).as("stage switch state").isTrue();

            assertThat(switchMode.getState()).as("mode after the normal cycle").isTrue();
        }

        {
            // Boom! We lose power.

            switchMode.setState(false);
        }

        {
            // Switch off after power loss

            driver.setFanSpeed(0);
            driver.setStage(0);

            double[] fanSpeed = driver.getFanSpeed();
            int[] stage = driver.getStage();

            assertThat(fanSpeed[0]).as("expected fan speed").isEqualTo(0.0);
            assertThat(fanSpeed[1]).as("actual fan speed").isEqualTo(0.0);
            assertThat(switchFan.getState()).as("fan speed switch state").isFalse();
            assertThat(stage[0]).as("expected stage").isZero();
            assertThat(stage[1]).as("actual stage").isZero();
            assertThat(switchFan.getState()).as("stage switch state").isFalse();

            // Not checking this switch now, irrelevant
            //assertThat(switchMode.getState()).as("mode after the normal cycle").isTrue();
        }

        {
            // Cycle after power loss.

            // All the switches are in their default ("failsafe") positions. We don't care about the fan speed
            // and the stage at this point because they will be forcibly set up to desired value anyway,
            // but we *do* care about the mode switch being in the proper position. If it is not,
            // we get a positive feedback runaway loop - at best, we'll some money and some comfort,
            // at worst, catastrophic meltdown - the system will just keep running until manually interrupted.

            // Let's make sure the switch is still in its "power loss" state

            assertThat(switchMode.getState()).as("'power loss' mode switch state").isFalse();

            driver.setFanSpeed(1);
            driver.setStage(1);

            double[] fanSpeed = driver.getFanSpeed();
            int[] stage = driver.getStage();

            assertThat(fanSpeed[0]).as("expected fan speed").isEqualTo(1.0);
            assertThat(fanSpeed[1]).as("actual fan speed").isEqualTo(1.0);
            assertThat(stage[0]).as("expected stage").isEqualTo(1);
            assertThat(stage[1]).as("actual stage").isEqualTo(1);

            assertThat(switchMode.getState()).as("mode after the power loss cycle").isTrue();
        }
    }

    @Test
    void nullMode() throws IOException {

        var switchMode = new NullSwitch("mode");
        var switchRunning = new NullSwitch("running");
        var switchFan = new NullSwitch("fan");

        var driver = new HvacDriverHeatpump(switchMode, switchRunning, switchFan);

        // The mode is not set, but we're shutting it off, that's OK

        driver.setStage(0);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> driver.setStage(1))
                .withMessage("mode can't be null");
    }
}
