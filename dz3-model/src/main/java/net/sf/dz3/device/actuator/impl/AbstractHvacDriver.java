package net.sf.dz3.device.actuator.impl;

import net.sf.dz3.device.actuator.HvacDriver;
import net.sf.dz3.device.model.HvacMode;
import net.sf.dz3.device.model.impl.HvacDriverSignal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.io.IOException;

/**
 * Abstract HVAC driver.
 *
 * Provides common logic for state housekeeping and JMX reporting.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public abstract class AbstractHvacDriver implements HvacDriver {

    protected final Logger logger = LogManager.getLogger();

    private HvacState expected;
    private HvacState actual;

    protected AbstractHvacDriver() {

        expected = new HvacState();
        actual = new HvacState();
    }

    @Override
    public final double[] getFanSpeed() {
        return new double[] {expected.speed, actual.speed};
    }

    @Override
    public final HvacMode[] getMode() {
        return new HvacMode[] {expected.mode, actual.mode};
    }

    @Override
    public final int[] getStage() {
        return new int[] {expected.stage, actual.stage};
    }

    @Override
    public final void setMode(HvacMode mode) throws IOException {

        ThreadContext.push("setMode");

        try {

            logger.info("mode={}", mode);
            expected = new HvacState(mode, expected.stage, expected.speed);

            doSetMode(mode);

            actual = new HvacState(mode, actual.stage, actual.speed);

        } finally {
            ThreadContext.pop();
        }
    }

    @Override
    public final void setStage(int stage) throws IOException {

        ThreadContext.push("setStage");

        try {

            logger.info("stage={}", stage);
            expected = new HvacState(expected.mode, stage, expected.speed);

            doSetStage(stage);

            actual = new HvacState(actual.mode, stage, actual.speed);

        } finally {
            ThreadContext.pop();
        }
    }

    @Override
    public final void setFanSpeed(double speed) throws IOException {

        ThreadContext.push("setFanSpeed");

        try {

            logger.info("fanSpeed={}", speed);
            expected = new HvacState(expected.mode, expected.stage, speed);

            doSetFanSpeed(speed);

            actual = new HvacState(actual.mode, actual.stage, speed);

        } finally {
            ThreadContext.pop();
        }
    }

    protected abstract  void doSetMode(HvacMode mode) throws IOException;

    protected abstract void doSetStage(int stage) throws IOException;

    protected abstract void doSetFanSpeed(double speed) throws IOException;

    protected static class HvacState {

        public final HvacMode mode;
        public final int stage;
        public final double speed;

        public HvacState() {
            mode = HvacMode.OFF;
            stage = 0;
            speed = 0;
        }

        public HvacState(HvacMode mode, int stage, double speed) {

            if (stage < 0) {
                // Hmm... what if it is much greater than 0? :O
                throw new IllegalArgumentException("stage can't be negative (" + stage + " provided)");
            }

            if (speed < 0 || speed > 1) {
                throw new IllegalArgumentException("speed outside of 0..1 range (" + speed + " provided)");
            }

            this.mode = mode;
            this.stage = stage;
            this.speed = speed;
        }
    }

    @Override
    public HvacDriverSignal getSignal() {
        return new HvacDriverSignal(getMode(), getStage(), getFanSpeed());
    }
}
