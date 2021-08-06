package net.sf.dz3r.controller.pid;

import com.homeclimatecontrol.jukebox.jmx.JmxAttribute;
import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;
import net.sf.dz3r.controller.AbstractProcessController;
import net.sf.dz3r.signal.Signal;

/**
 * Abstract base for a PID controller implementation.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public abstract class AbstractPidController extends AbstractProcessController<Double, Double> implements PidController {

    /**
     * Indicates whether to reset the {@link #getIntegral() accumulated integral component}
     * upon setpoint change.
     */
    private boolean resetOnSetpointChange = true;

    /**
     * Proportional weight.
     */
    private double P;

    /**
     * Integral weight.
     */
    private double I;

    /**
     * Derivative weight.
     */
    private double D;

    /**
     * Saturation limit. The controller is considered saturated if the output
     * absolute value is greater than this.
     */
    private double saturationLimit;

    /**
     * Last known value of proportional component.
     *
     * This value is here for instrumentation purposes only.
     */
    private double lastP = 0;

    /**
     * Last known value of integral component.
     *
     * This value is here for instrumentation purposes only.
     */
    private double lastI = 0;

    /**
     * Last known value of derivative component.
     *
     * This value is here for instrumentation purposes only.
     */
    private double lastD = 0;

    protected AbstractPidController(String jmxName, final double setpoint, final double P, final double I, final double D, double saturationLimit) {

        super(jmxName, setpoint);

        setP(P);
        setI(I);
        setD(D);
        setLimit(saturationLimit);

        check();
    }

    @Override
    protected double getError(Signal<Double> pv, double setpoint) {
        return pv.getValue() - setpoint;
    }

    @Override
    protected Signal<Status<Double>> wrapCompute(Signal<Double> pv) {
        // This will only be non-null upon second invocation
        var lastOutputSignal = getLastOutputSignal();

        var error = getError();
        var p = error * getP();
        lastP = p;
        var signal = p;

        if (saturationLimit == 0) {
            var integral = getIntegral(lastOutputSignal, pv, error);
            lastI = integral * getI();

        } else {

            if (lastOutputSignal != null && Math.abs(lastOutputSignal.getValue().signal) < saturationLimit) {

                // Integral value will only be updated if the output is not saturated
                var integral = getIntegral(lastOutputSignal, pv, error);

                // And only if the integral itself is not saturated either
                if (Math.abs(integral * getI()) < saturationLimit) {
                    lastI = integral * getI();
                } else {
                    // lastI stays where it was. Not perfect, but at least predictable.
                }

            } else {

                // This is necessary for stateful integrators. The error is 0, but other things might change.
                getIntegral(lastOutputSignal, pv, 0);
            }
        }

        signal += lastI;

        var derivative = getDerivative(lastOutputSignal, pv, error);

        // One cause of this is setSetpoint(), which causes values
        // to be computed in a rapid succession, with chance of delta T being zero
        // being close to 1
        if (Double.compare(derivative, Double.NaN) != 0
                && Double.compare(derivative, Double.NEGATIVE_INFINITY) != 0
                && Double.compare(derivative, Double.POSITIVE_INFINITY) != 0) {

            lastD = derivative * getD();
            signal += lastD;
        }

        // VT: NOTE: When the hell was it NaN? I know this code wouldn't be
        // here for no reason, but can't remember the circumstances.
        // Need them to write the test case.

        // VT: NOTE: Aha, one such case is right above. Need to see if this ever happens again.

        if (Double.compare(signal, Double.NaN) == 0) {
            throw new IllegalStateException("signal is NaN, components: "
                    + new PidStatus(
                    new Status<>(getSetpoint(), error, signal),
                    lastP, lastI, lastD));
        }

        return new Signal<>(pv.timestamp,
                new PidStatus(
                        new Status<>(getSetpoint(), error, signal),
                        lastP, lastI, lastD),
                pv.status,
                pv.error);
    }

    /**
     * Check if  parameters are fine.
     */
    private void check() {

        // This method should be fast because it will be called at the
        // beginning of compute() to make sure the controller is initialized

        if (P == 0 && I == 0 && D == 0) {
            throw new IllegalArgumentException("All PID components are zeroed: check the configuration");
        }

        if (saturationLimit < 0) {
            throw new IllegalArgumentException("Check parameters: saturationLimit=" + saturationLimit);
        }
    }

    @Override
    public void setP(double P) {

        this.P = P;
        configurationChanged();
    }

    @Override
    public void setI(double I) {

        this.I = I;
        configurationChanged();
    }

    @Override
    public void setD(double D) {
        this.D = D;
        configurationChanged();
    }

    @Override
    public void setLimit(double saturationLimit) {

        if (saturationLimit < 0) {
            throw new IllegalArgumentException("limit must be non-negative");
        }

        this.saturationLimit = saturationLimit;
        configurationChanged();
    }

    @Override
    public final double getP() {
        return P;
    }

    @Override
    public final double getI() {
        return I;
    }

    @Override
    public final double getD() {
        return D;
    }

    @Override
    @JmxAttribute(description = "Integral component saturation limit")
    public final double getLimit() {
        return saturationLimit;
    }

    @Override
    public JmxDescriptor getJmxDescriptor() {
        return new JmxDescriptor(
                "dz",
                "PID Controller",
                jmxName,
                "Emits control signal based on (P, I, D, Limit) values");
    }


    protected abstract double getIntegral(Signal<Status<Double>> lastKnownSignal, Signal<Double>  pv, double error);
    protected abstract double getDerivative(Signal<Status<Double>> lastKnownSignal, Signal<Double>  pv, double error);

    @JmxAttribute(description = "Accumulated integral component")
    public final double getIntegral() {
        return lastI;
    }

    @JmxAttribute(description = "Whether to reset the accumulated integral component upon setpoint change")
    public boolean getResetOnSetpointChange() {
        return resetOnSetpointChange;
    }

    public void setResetOnSetpointChange(boolean reset) {
        this.resetOnSetpointChange = reset;
    }
}
