package net.sf.dz3r.controller;

import com.homeclimatecontrol.jukebox.jmx.JmxAttribute;
import net.sf.dz3r.signal.Signal;

/**
 * A hysteresis controller.
 *
 * The controller output becomes positive when the process variable becomes
 * higher than the setpoint plus hysteresis, and it becomes negative when the
 * process variable becomes lower than the setpoint minus hysteresis.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class HysteresisController<A extends Comparable<A>> extends AbstractProcessController<A> {

    public static final double DEFAULT_HYSTERESIS = 1;

    /**
     * {@link #getSetpoint()} + {@code thresholdHigh} is the upper tipping point.
     */
    private double thresholdHigh;

    /**
     * {@link #getSetpoint()} - {@code thresholdLow} is the lower tipping point.
     */
    private double thresholdLow;

    /**
     * The controller state. Initial state is off.
     */
    private boolean state = false;

    /**
     * Create an instance with default hysteresis value.
     *
     * @param setpoint Initial setpoint.
     */
    protected HysteresisController(double setpoint) {
        this(setpoint, -DEFAULT_HYSTERESIS, DEFAULT_HYSTERESIS);
    }

    /**
     * Create an instance.
     *
     * @param setpoint Initial setpoint.
     * @param hysteresis Initial hysteresis.
     */
    public HysteresisController(double setpoint, double hysteresis) {

        this(setpoint, -hysteresis, hysteresis);
    }

    /**
     *
     * @param setpoint Initial setpoint.
     * @param thresholdLow Lower tipping point.
     * @param thresholdHigh Upper tipping point.
     */
    public HysteresisController(double setpoint, double thresholdLow, double thresholdHigh) {

        super(setpoint);

        setLow(thresholdLow);
        setHigh(thresholdHigh);
    }

    /**
     * Set {@link #thresholdLow}.
     *
     * @param thresholdLow Desired low threshold.
     *
     * @exception IllegalArgumentException if the value is non-negative.
     */
    private void setLow(double thresholdLow) {

        if (thresholdLow >= 0) {
            throw new IllegalArgumentException("Low threshold must be negative (value given is " + thresholdLow + ")");
        }

        this.thresholdLow = thresholdLow;
    }

    /**
     * Set {@link #thresholdHigh}.
     *
     * @param thresholdHigh Desired high threshold.
     *
     * @exception IllegalArgumentException if the value is non-positive.
     */
    private void setHigh(double thresholdHigh) {

        if (thresholdHigh <= 0) {
            throw new IllegalArgumentException("High threshold must be positive(value given is " + thresholdHigh + ")");
        }

        this.thresholdHigh = thresholdHigh;
    }

    /**
     * @return Current value of low threshold.
     */
    @JmxAttribute(description = "threshold.low")
    public double getThresholdLow() {
        return thresholdLow;
    }

    /**
     * @return Current value of high threshold.
     */
    @JmxAttribute(description = "threshold.high")
    public double getThresholdHigh() {
        return thresholdHigh;
    }

    @Override
    protected Signal<A, Double> wrapCompute(Signal<A, Double> pv) {

        if (pv == null) {
            // Don't have to do a thing, nothing happened yet, nothing good at least
            return null;
        }

        if (pv.isError()) {
            // Nothing good at least; let's pass up the error
            return pv;
        }

        if (pv.getValue().isEmpty()) {
            throw new IllegalStateException("empty state, not error, can't be: " + pv);
        }

        var sample = pv.getValue().get(); // NOSONAR Sonar bug

        if (state) {

            if (sample - thresholdLow <= getSetpoint()) {
                state = false;
            }

        } else {

            if (sample - thresholdHigh >= getSetpoint()) {
                state = true;
            }
        }

        return new Signal<>(pv.timestamp, pv.address, state ? DEFAULT_HYSTERESIS : -DEFAULT_HYSTERESIS);
    }
}
