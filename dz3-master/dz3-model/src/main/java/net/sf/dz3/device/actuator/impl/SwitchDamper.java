package net.sf.dz3.device.actuator.impl;

import java.io.IOException;

import org.apache.logging.log4j.ThreadContext;

import net.sf.dz3.device.sensor.Switch;
import net.sf.dz3.instrumentation.Marker;
import net.sf.jukebox.jmx.JmxAttribute;
import net.sf.jukebox.jmx.JmxDescriptor;

/**
 * Damper controlled by a switch.
 *
 * Most existing HVAC dampers are like this, controlled by 24VAC.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com"> Vadim Tkachenko</a> 2001-2020
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
     * Heartbeat interval.
     *
     * Command to set the state will not be sent to {@link #target} if it was last
     * sent less than this many milliseconds ago.
     */
    private long heartbeatMillis;

    /**
     * Last time the command was successfully sent to the {@link #target}.
     *
     * Set in {@link #moveDamper(double)}.
     */
    private long lastKnown = 0;

    /**
     * Last known target state.
     *
     * Set in {@link #moveDamper(double)}.
     */
    private boolean targetState;

    /**
     * Create an instance with default (1.0) park position.
     *
     * @param name Damper name. Necessary evil to allow instrumentation signature.
     * @param target Switch that controls the actual damper.
     * @param threshold Switch threshold.
     * @param heartbeatSeconds The command to set the position will not be sent to the actual switch
     * more often than once in this many seconds.
     */
    public SwitchDamper(String name, Switch target, double threshold, int heartbeatSeconds) {

        this(name, target, threshold, 1.0, heartbeatSeconds, false);
    }

    /**
     * Create an instance.
     *
     * @param name Damper name. Necessary evil to allow instrumentation signature.
     * @param target Switch that controls the actual damper.
     * @param threshold Switch threshold.
     * @param parkPosition Damper position defined as 'parked'.
     * @param heartbeatSeconds The command to set the position will not be sent to the actual switch
     * more often than once in this many seconds.
     */
    public SwitchDamper(String name, Switch target, double threshold, double parkPosition, int heartbeatSeconds) {

        this(name, target, threshold, parkPosition, heartbeatSeconds, false);
    }

    /**
     * Create an instance.
     *
     * @param name Damper name. Necessary evil to allow instrumentation signature.
     * @param target Switch that controls the actual damper.
     * @param threshold Switch threshold.
     * @param parkPosition Damper position defined as 'parked'.
     * @param heartbeatSeconds The command to set the position will not be sent to the actual switch
     * more often than once in this many seconds.
     * @param inverted {@code true} if the switch position needs to be inverted.
     */
    public SwitchDamper(String name, Switch target, double threshold, double parkPosition, int heartbeatSeconds, boolean inverted) {

        super(name);

        check(target);
        check(threshold);

        this.target = target;
        this.threshold = threshold;

        this.inverted = inverted;

        if (heartbeatSeconds < 0) {
            throw new IllegalArgumentException("hearbeatSeconds must be positive (received " + heartbeatSeconds + ")");
        }

        heartbeatMillis = heartbeatSeconds * 1000L;

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

    @Override
    public void moveDamper(double position) throws IOException {

        ThreadContext.push("moveDamper");
        Marker m = new Marker("moveDamper");

        try {

            boolean state = position > threshold;

            state = inverted ? !state : state;

            logger.debug("translated {} => {}{}", position, state, (inverted ? " (inverted)" : ""));

            // VT: NOTE: This call makes it a royal PITA to troubleshoot;
            // might want to think of injectable TimeSource
            long now = System.currentTimeMillis();
            long elapsed = now - lastKnown;

            if (elapsed > heartbeatMillis || state != targetState) {

                if (state == targetState) {
                    // It's a timeout, then
                    logger.debug("heartbeat exceeded by {}ms, invoking hardware", elapsed - heartbeatMillis);
                }

                target.setState(state);

                // This will not be set if the command failed
                lastKnown = now;
                targetState = state;
            }

            this.position = position;

        } catch (Throwable t) {

            // This is pretty serious, closed damper may cause the compressor to slug
            // or the boiler to blow up - so no harm in logging this multiple times, hopefully

            logger.fatal("failed to set state for {}", target.getAddress(), t);

            throw new IOException("failed to set state for " + target.getAddress(), t);

        } finally {

            m.close();
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
                "Controls a switch that controls a damper");
    }

    @JmxAttribute(description = "threshold")
    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }
}
