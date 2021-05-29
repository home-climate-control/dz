package net.sf.dz3.device.actuator.impl;

import com.homeclimatecontrol.jukebox.jmx.JmxAttribute;
import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;
import net.sf.dz3.device.sensor.Switch;
import org.apache.logging.log4j.ThreadContext;

import java.io.IOException;
import java.time.Duration;

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
     * Heartbeat interval.
     *
     * Command to set the state will not be sent to {@link #target} if it was last
     * sent less than this much ago.
     */
    private Duration heartbeat = Duration.ofMillis(0);

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
     * Create a non-inverted instance.
     *
     * @param name Damper name. Necessary evil to allow instrumentation signature.
     * @param target Switch that controls the actual damper.
     * @param threshold Switch threshold.
     * @param parkPosition Damper position defined as 'parked'.
     */
    public SwitchDamper(String name, Switch target, double threshold, double parkPosition, boolean inverted) {
        this(name, target, threshold, parkPosition, inverted, 0);
    }

    /**
     * Create an instance.
     *
     * @param name Damper name. Necessary evil to allow instrumentation signature.
     * @param target Switch that controls the actual damper.
     * @param threshold Switch threshold.
     * @param parkPosition Damper position defined as 'parked'.
     * @param inverted {@code true} if the switch is inverted.
     * @param heartbeatSeconds Set the {@link #heartbeat} to this interval in seconds.
     */
    public SwitchDamper(String name, Switch target, double threshold, double parkPosition, boolean inverted, long heartbeatSeconds) {
        super(name);

        check(target);
        check(threshold);

        this.target = target;
        this.threshold = threshold;
        this.inverted = inverted;

        setParkPosition(parkPosition);
        setHeartbeatSeconds(heartbeatSeconds);

        set(getParkPosition());
    }

    @JmxAttribute(description = "Heartbeat duration")
    public Duration getHeartbeat() {
        return heartbeat;
    }

    public void setHeartbeat(Duration heartbeat) {

        if (heartbeat.isNegative()) {
            throw new IllegalArgumentException("negative heartbeat not acceptable: " + heartbeat);
        }

        this.heartbeat = heartbeat;
    }

    @JmxAttribute(description = "Heartbeat duration in seconds")
    public long getHeartbeatSeconds() {
        return heartbeat.getSeconds();
    }

    public void setHeartbeatSeconds(long heartbeatSeconds) {
        setHeartbeat(Duration.ofSeconds(heartbeatSeconds));
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

            state = inverted != state;

            logger.debug("translated {} => {}{}", position, state, (inverted ? " (inverted)" : ""));

            setState(state);

            this.position = position;

        } catch (Throwable t) {

            // squid:S1181: No.
            // This is pretty serious, closed damper may cause the compressor to slug
            // or the boiler to blow up - so no harm in logging this multiple times, hopefully
            logger.fatal("failed to set state for {}", target.getAddress(), t);

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Set the {@link #target} state, but only if it is beyond the {@link #heartbeat}.
     *
     * @param state State to set the {@link #target} to.
     */
    private void setState(boolean state) throws IOException {

        // VT: NOTE: This call makes it a royal PITA to troubleshoot;
        // might want to think of injectable TimeSource
        long now = System.currentTimeMillis();
        long elapsed = now - lastKnown;

        if (elapsed > heartbeat.toMillis() || state != targetState) {

            if (state == targetState) {
                // It's a timeout, then
                logger.debug("heartbeat exceeded by {}ms, invoking hardware", elapsed - heartbeat.toMillis());
            }

            target.setState(state);

            // This will not be set if the command failed
            lastKnown = now;
            targetState = state;
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
