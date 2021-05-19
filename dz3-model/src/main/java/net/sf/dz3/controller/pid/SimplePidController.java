package net.sf.dz3.controller.pid;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;

/**
 * Simple stateless PID controller implementation.
 * 
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2009
 */
public class SimplePidController extends AbstractPidController {

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

    public SimplePidController(double setpoint, double P, double I, double D, double saturationLimit) {
        super(setpoint, P, I, D, saturationLimit);
    }

    @Override
    protected final double getIntegral(DataSample<Double> lastKnownSignal, DataSample<Double>  pv, double error) {

        if (lastKnownSignal == null) {
            return integral;
        }

        long deltaT = pv.timestamp - lastKnownSignal.timestamp;
        integral += error * deltaT;

        return integral;
    }

    @Override
    protected final double getDerivative(DataSample<Double> lastKnownSignal, DataSample<Double>  pv, double error) {

        if (lastKnownSignal == null) {
            return 0;
        }

        long deltaT = pv.timestamp - lastKnownSignal.timestamp;

        // deltaT is guaranteed not to be 0 - see call stack
        double derivative = (error - lastError) / deltaT;
        lastError = error;

        return derivative;
    }

    @Override
    protected void setpointChanged() {
        
        if (needResetOnSetpointChange()) {
            
            integral = 0;
        }
    }
}
