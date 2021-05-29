package net.sf.dz3.device.actuator.impl;

import com.homeclimatecontrol.jukebox.jmx.JmxAttribute;
import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;
import net.sf.dz3.device.sensor.Switch;
import org.apache.logging.log4j.ThreadContext;

import java.io.IOException;

/**
 * Damper controlled by a switch.
 *
 * Most existing HVAC dampers are like this, controlled by 24VAC.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2018
 */
public class SwitchDamper extends AbstractDamper {

    /**
     * Current position.
     */
    private double position;

    /**
     * Hardware switch that controls the actual damper.
     */
    private final Switch target;

    /**
     * Switch threshold.
     *
     * Values passed to {@link #moveDamper(double)} above the threshold
     * will set the switch to 1,values equal or less will set the switch to 0.
     */
    private double threshold;
    /**
     * If {@code} true, then {@code 1.0} damper position would mean the switch in {@code false} state,
     * not in {@code true} like it normally would.
     */
    private final boolean inverted;

    /**
     * Create a non-inverted instance with default (1.0) park position.
     *
     * @param name Damper name. Necessary evil to allow instrumentation signature.
     * @param target Switch that controls the actual damper.
     * @param threshold Switch threshold.
     */
    public SwitchDamper(String name, Switch target, double threshold) {
        this(name, target, threshold, 1.0, false);
    }

    /**
     * Create a non-inverted instance.
     *
     * @param name Damper name. Necessary evil to allow instrumentation signature.
     * @param target Switch that controls the actual damper.
     * @param threshold Switch threshold.
     * @param parkPosition Damper position defined as 'parked'.
     */
    public SwitchDamper(String name, Switch target, double threshold, double parkPosition) {
        this(name, target, threshold, parkPosition, false);
    }
    /**
     * Create an instance.
     *
     * @param name Damper name. Necessary evil to allow instrumentation signature.
     * @param target Switch that controls the actual damper.
     * @param threshold Switch threshold.
     * @param parkPosition Damper position defined as 'parked'.
     */
    public SwitchDamper(String name, Switch target, double threshold, double parkPosition, boolean inverted) {
        super(name);

        check(target);
        check(threshold);

        this.target = target;
        this.threshold = threshold;
        this.inverted = inverted;

        setParkPosition(parkPosition);

        set(getParkPosition());
    }

    private void check(Switch target) {

        if (target == null) {
            throw new IllegalArgumentException("target can't be null");
        }
    }

    private void check(double threshold) {

        if (threshold <= 0 || threshold >= 1.0 ) {
            throw new IllegalArgumentException("Unreasonable threshold value given ("
                    + threshold + "), valid values are (0 < threshold < 1)");
        }
    }

    @SuppressWarnings("squid:S1181")
    @Override
    public void moveDamper(double position) {

        ThreadContext.push("moveDamper");

        try {

            boolean state = position > threshold;

            state = inverted ? !state : state;

            logger.debug("translated {} => {}", position, state);

            target.setState(state);

            this.position = position;

        } catch (Throwable t) {

            // squid:S1181: No.
            // This is pretty serious, closed damper may cause the compressor to slug
            // or the boiler to blow up
            logger.fatal("failed to set state for {}", target.getAddress(), t);

        } finally {
            ThreadContext.pop();
        }
    }

    @Override
    public double getPosition() throws IOException {
        return position;
    }

    @Override
    public JmxDescriptor getJmxDescriptor() {

        return new JmxDescriptor(
                "dz",
                "Switch based damper",
                Integer.toHexString(hashCode()),
                "Controls switch " + target.getAddress() + " that controls a damper");
    }

    @JmxAttribute(description = "threshold")
    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        check(threshold);
        this.threshold = threshold;
    }
}
