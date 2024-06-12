package net.sf.dz3r.controller;

import net.sf.dz3r.signal.Signal;

/**
 * A hysteresis controller.
 *
 * The controller output becomes positive when the process variable becomes
 * higher than the setpoint plus hysteresis, and it becomes negative when the
 * process variable becomes lower than the setpoint minus hysteresis.
 *
 * @param <P> Payload type.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
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
    public double getThresholdLow() {
        return thresholdLow;
    }

    /**
     * Get the high threshold.
     *
     * @return Current value of high threshold.
     */
    public double getThresholdHigh() {
        return thresholdHigh;
    }

    @Override
    protected double getError(Signal<Double, P> pv, Double setpoint) {
        return pv.getValue() - setpoint;
    }

    @Override
    protected Signal<Status<Double>, P> wrapCompute(Double setpoint, Signal<Double, P> pv) {

        if (pv == null) {
            // This should've been handled up the call stack already, but let's be paranoid
            throw new IllegalArgumentException("pv can't be null");
        }

        if (pv.isError()) {
            // Nothing good at least; let's pass up the error
            return new Signal<>(pv.timestamp, new HysteresisStatus(setpoint, null, null, null), pv.payload, pv.status, pv.getError());
        }

        var sample = pv.getValue(); // NOSONAR false positive

        if (sample == null) {
            throw new IllegalStateException("empty state, not error, can't be: " + pv);
        }

        var oldState = state;

        if (state) {

            if (sample - thresholdLow <= setpoint) {
                state = false;
            }

        } else {

            if (sample - thresholdHigh >= setpoint) {
                state = true;
            }
        }

        if (oldState != state) {
            logger.trace("{}: state change {} => {}", jmxName, oldState, state);
        }

        return new Signal<>(pv.timestamp, new HysteresisStatus(setpoint, getError(), state ? DEFAULT_HYSTERESIS : -DEFAULT_HYSTERESIS, sample), pv.payload);
    }

    @Override
    protected void configurationChanged() {
        // Do nothing yet
    }

    public static class HysteresisStatus extends Status<Double> {

        public final Double sample;

        public HysteresisStatus(double setpoint, Double error, Double signal, Double sample) {
            super(setpoint, error, signal);

            this.sample = sample;
        }

        @Override
        public String toString() {
            return "setpoint=" + setpoint + ",error=" + error + ",signal=" + signal + ",sample=" + sample;
        }
    }
}
