package net.sf.dz3.controller.pid;

import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.jmx.JmxAttribute;

/**
 * Classical PID - Proportional, Integral, Derivative controller.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko</a> 2001-2009
 */
public class PID_Controller extends AbstractPidController implements PidControllerConfiguration {

    /**
     * Integral data set.
     */
    private IntegralSet integralSet;

    /**
     * Differential data set.
     */
    private DifferentialSet differentialSet;

    /**
     * Create the configured instance.
     *
     * @param P Proportional weight.
     * @param I Integral weight.
     * @param Ispan Integral timespan, milliseconds.
     * @param D Derivative weight.
     * @param Dspan Derivative timespan, milliseconds.
     * @param saturationLimit Anti-windup control value. The controller is
     * considered saturated if the output absolute value is greater than this.
     * Zero means no anti-windup will be provided.
     */
    public PID_Controller(final double setpoint, final double P, final double I, final long Ispan, final double D, final long Dspan,
            final double saturationLimit) {
	
	super(setpoint, P, I, D, saturationLimit);

        this.integralSet = new LegacyIntegralSet(Ispan);
        this.differentialSet = new DifferentialSet(Dspan);
    }

    @JmxAttribute(description = "Proportional component time span")
    public void setIspan(long iSpan) {

      // VT: FIXME: This will reset the existing set and screw things up
      if (getI() != 0) {
          integralSet = new LegacyIntegralSet(iSpan);
      }
      statusChanged();
    }

    @JmxAttribute(description = "Derivative component time span")
    public void setDspan(long dSpan) {

      // VT: FIXME: This will reset the existing set and screw things up
      if (getD() != 0) {
          differentialSet = new DifferentialSet(dSpan);
      }
      statusChanged();
    }

    @Override
    protected double getIntegral(DataSample<Double> lastKnownSignal, DataSample<Double>  pv, double error) {

        integralSet.record(getProcessVariable().timestamp, error);

        return integralSet.getIntegral();
    }

    @Override
    protected double getDerivative(DataSample<Double> lastKnownSignal, DataSample<Double>  pv, double error) {
        
        differentialSet.record(getProcessVariable().timestamp, error);

        return differentialSet.getDifferential();
    }

    @Override
    protected void setpointChanged() {
        
        // Do absolutely nothing
    }
}
