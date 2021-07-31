package net.sf.dz3.controller.pid;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;

/**
 * Classical PID - Proportional, Integral, Derivative controller.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class PID_Controller extends AbstractPidController implements PidControllerConfiguration {

    /**
     * Integral data set.
     */
    private SlidingIntegralSet integralSet;

    /**
     * Differential data set.
     */
    private SlidingDifferentialSet differentialSet;

    public PID_Controller(final double setpoint, final double P, final double I, final long Ispan, final double D, final long Dspan,
                          final double saturationLimit) {
        this(null, setpoint, P, I, Ispan, D, Dspan, saturationLimit);
    }

    /**
     * Create the configured instance.
     *
     * @param jmxName The name under which this object will be visible to JMX console.
     * @param P Proportional weight.
     * @param I Integral weight.
     * @param Ispan Integral timespan, milliseconds.
     * @param D Derivative weight.
     * @param Dspan Derivative timespan, milliseconds.
     * @param saturationLimit Anti-windup control value. The controller is
     * considered saturated if the output absolute value is greater than this.
     * Zero means no anti-windup will be provided.
     */
    public PID_Controller(String jmxName, final double setpoint, final double P, final double I, final long Ispan, final double D, final long Dspan,
                          final double saturationLimit) {

        super(jmxName, setpoint, P, I, D, saturationLimit);

        this.integralSet = new SlidingIntegralSet(Ispan);
        this.differentialSet = new SlidingDifferentialSet(Dspan);
    }

    @Override
    public long getIspan() {
        return integralSet.getSpan();
    }

    @Override
    public void setIspan(long iSpan) {

        // VT: FIXME: This will reset the existing set and screw things up
        if (getI() != 0) {
            integralSet = new SlidingIntegralSet(iSpan);
        }
        statusChanged();
    }

    @Override
    public long getDspan() {
        return differentialSet.getSpan();
    }

    @Override
    public void setDspan(long dSpan) {

        // VT: FIXME: This will reset the existing set and screw things up
        if (getD() != 0) {
            differentialSet = new SlidingDifferentialSet(dSpan);
        }
        statusChanged();
    }

    @Override
    protected double getIntegral(DataSample<Double> lastKnownSignal, DataSample<Double>  pv, double error) {

        integralSet.append(getProcessVariable().timestamp, error);
        return integralSet.getIntegral();
    }

    @Override
    protected double getDerivative(DataSample<Double> lastKnownSignal, DataSample<Double>  pv, double error) {

        differentialSet.append(getProcessVariable().timestamp, error);
        return differentialSet.getDifferential();
    }

    @Override
    protected void setpointChanged() {
        // Do absolutely nothing
    }
}
