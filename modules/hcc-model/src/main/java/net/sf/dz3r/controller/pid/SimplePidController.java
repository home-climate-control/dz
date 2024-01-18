package net.sf.dz3r.controller.pid;

import net.sf.dz3r.signal.Signal;

/**
 * Simple stateless reactive PID controller implementation.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class SimplePidController<P> extends AbstractPidController<P> {

    /**
     * I component.
     */
    private double integral = 0;

    /**
     * Last known error - need to calculate the D component.
     */
    private double lastError = 0;

    public SimplePidController(String jmxName, double setpoint, double P, double I, double D, double saturationLimit) {
        super(jmxName, setpoint, P, I, D, saturationLimit);
    }

    @Override
    protected final double getIntegral(Signal<Status<Double>, P> lastKnownSignal, Signal<Double, P>  pv, double error) {

        if (lastKnownSignal == null) {
            return integral;
        }

        long deltaT = pv.timestamp.toEpochMilli() - lastKnownSignal.timestamp.toEpochMilli();
        integral += error * deltaT;

        return integral;
    }

    @Override
    protected final double getDerivative(Signal<Status<Double>, P> lastKnownSignal, Signal<Double, P>  pv, double error) {

        if (lastKnownSignal == null) {
            return 0;
        }

        long deltaT = pv.timestamp.toEpochMilli() - lastKnownSignal.timestamp.toEpochMilli();

        // deltaT is guaranteed not to be 0 - see call stack
        double derivative = (error - lastError) / deltaT;
        lastError = error;

        return derivative;
    }

    @Override
    protected void configurationChanged() {

        if (getResetOnSetpointChange()) {
            integral = 0;
        }
    }
}
