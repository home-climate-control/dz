package net.sf.dz3r.controller;

import com.homeclimatecontrol.jukebox.jmx.JmxAttribute;
import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;
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
public class HysteresisController<P> extends AbstractProcessController<Double, Double, P> {

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
     * @param jmxName This controller's JMX name.
     * @param setpoint Initial setpoint.
     */
    public HysteresisController(String jmxName, double setpoint) {
        this(jmxName, setpoint, -DEFAULT_HYSTERESIS, DEFAULT_HYSTERESIS);
    }

    /**
     * Create an instance with a symmetrical hysteresis loop.
     *
     * @param jmxName This controller's JMX name.
     * @param setpoint Initial setpoint.
     * @param hysteresis Initial hysteresis.
     */
    public HysteresisController(String jmxName, double setpoint, double hysteresis) {
        this(jmxName, setpoint, -hysteresis, hysteresis);
    }

    /**
     * Create an instance with a custom hysteresis loop.
     *
     * @param jmxName This controller's JMX name.
     * @param setpoint Initial setpoint.
     * @param thresholdLow Lower tipping point.
     * @param thresholdHigh Upper tipping point.
     */
    public HysteresisController(String jmxName, double setpoint, double thresholdLow, double thresholdHigh) {

        super(jmxName, setpoint);

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
     * Get the low threshold.
     *
     * @return Current value of low threshold.
     */
    @JmxAttribute(description = "threshold.low")
    public double getThresholdLow() {
        return thresholdLow;
    }

    /**
     * Get the high threshold.
     *
     * @return Current value of high threshold.
     */
    @JmxAttribute(description = "threshold.high")
    public double getThresholdHigh() {
        return thresholdHigh;
    }

    @Override
    protected double getError(Signal<Double, P> pv, double setpoint) {
        return pv.getValue() - setpoint;
    }

    @Override
    protected Signal<Status<Double>, P> wrapCompute(Signal<Double, P> pv) {

        if (pv == null) {
            // This should've been handled up the call stack already, but let's be paranoid
            throw new IllegalArgumentException("pv can't be null");
        }

        if (pv.isError()) {
            // Nothing good at least; let's pass up the error
            return new Signal<>(pv.timestamp, new Status(getSetpoint(), null, null), pv.payload, pv.status, pv.getError());
        }

        var sample = pv.getValue(); // NOSONAR false positive

        if (sample == null) {
            throw new IllegalStateException("empty state, not error, can't be: " + pv);
        }

        if (state) {

            if (sample - thresholdLow <= getSetpoint()) {
                state = false;
            }

        } else {

            if (sample - thresholdHigh >= getSetpoint()) {
                state = true;
            }
        }

        return new Signal<>(pv.timestamp, new Status(getSetpoint(), getError(), state ? DEFAULT_HYSTERESIS : -DEFAULT_HYSTERESIS), pv.payload);
    }

    @Override
    protected void configurationChanged() {
        // Do nothing yet
    }

    @Override
    public JmxDescriptor getJmxDescriptor() {
        return new JmxDescriptor(
                "dz",
                "Hysteresis Controller",
                jmxName,
                "Emits hysteresis control signal");
    }
}
