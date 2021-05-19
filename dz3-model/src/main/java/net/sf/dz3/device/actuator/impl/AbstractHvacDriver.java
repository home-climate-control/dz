package net.sf.dz3.device.actuator.impl;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import net.sf.dz3.device.actuator.HvacDriver;
import net.sf.dz3.device.model.HvacMode;
import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;

/**
 * Abstract HVAC driver.
 *
 * Provides common logic for state housekeeping and JMX reporting.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2020
 */
public abstract class AbstractHvacDriver implements HvacDriver {

    protected final Logger logger = LogManager.getLogger(getClass());

    private HvacState expected;
    private HvacState actual;

    public AbstractHvacDriver() {

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

            logger.info("mode=" + mode);
            expected.mode = mode;

            doSetMode(mode);

            actual.mode = mode;

        } finally {
            ThreadContext.pop();
        }
    }

    @Override
    public final void setStage(int stage) throws IOException {

        ThreadContext.push("setStage");

        try {

            logger.info("stage=" + stage);
            expected.stage = stage;

            doSetStage(stage);

            actual.stage = stage;

        } finally {
            ThreadContext.pop();
        }
    }

    @Override
    public final void setFanSpeed(double speed) throws IOException {

        ThreadContext.push("setFanSpeed");

        try {

            logger.info("fanSpeed=" + speed);
            expected.speed = speed;

            doSetFanSpeed(speed);

            actual.speed = speed;

        } finally {
            ThreadContext.pop();
        }
    }

    protected abstract  void doSetMode(HvacMode mode) throws IOException;

    protected abstract void doSetStage(int stage) throws IOException;

    protected abstract void doSetFanSpeed(double speed) throws IOException;

    @Override
    public JmxDescriptor getJmxDescriptor() {

        return new JmxDescriptor(
                "dz",
                "Null HVAC Driver",
                Integer.toHexString(hashCode()),
                "Pretends to be the actual HVAC driver");
    }

    protected static class HvacState {

        public HvacMode mode;
        public int stage;
        public double speed;

        public HvacState() {

            mode = HvacMode.OFF;
            stage = 0;
            speed = 0;
        }
    }
}
