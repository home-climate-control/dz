package net.sf.dz3.controller.pid;

import org.apache.logging.log4j.ThreadContext;

import net.sf.dz3.controller.AbstractProcessController;
import net.sf.dz3.controller.ProcessControllerStatus;
import net.sf.dz3.util.digest.MessageDigestCache;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.jmx.JmxAttribute;
import com.homeclimatecontrol.jukebox.jmx.JmxAware;
import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;

/**
 * Abstract base for a PID controller implementation.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko</a> 2001-2018
 */
public abstract class AbstractPidController extends AbstractProcessController implements AbstractPidControllerConfiguration, JmxAware {

    /**
     * Name this object has in a {@link #getJmxDescriptor() JMX representation}.
     */
    private final String jmxName;
    
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

    public AbstractPidController(String jmxName, final double setpoint, final double P, final double I, final double D, double saturationLimit) {
	
        super(setpoint);

	if ("".equals(jmxName)) {
	    throw new IllegalArgumentException("jmxName can't be null or empty");
	}
	
	if (jmxName == null) {
	
	    this.jmxName = Integer.toHexString(hashCode());
	    
	} else {
	
	    this.jmxName = jmxName;
	}
	
        setP(P);
        setI(I);
        setD(D);
        setLimit(saturationLimit);

	check();
    }

    public AbstractPidController(final double setpoint, final double P, final double I, final double D, double saturationLimit) {
        
        this(null, setpoint, P, I, D, saturationLimit);
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
	statusChanged();
    }

    @Override
    public void setI(double I) {

	this.I = I;
	statusChanged();
    }

    @Override
    public void setD(double D) {

	this.D = D;
	statusChanged();
    }
    
    public void setLimit(double saturationLimit) {

        if (saturationLimit < 0) {
            throw new IllegalArgumentException("limit must be non-negative");
        }
        
        this.saturationLimit = saturationLimit;
        statusChanged();
    }

    @JmxAttribute(description = "Proportional weight")
    public final double getP() {
	return P;
    }

    @JmxAttribute(description = "Integral weight")
    public final double getI() {
	return I;
    }
    
    @JmxAttribute(description = "Derivative weight")
    public final double getD() {
	return D;
    }

    @JmxAttribute(description = "Integral component saturation limit")
    public final double getLimit() {
        return saturationLimit;
    }

    @Override
    protected final DataSample<Double> compute() {

        ThreadContext.push("compute");

        try {

            // This is guaranteed to be not null (see call stack)
            DataSample<Double> pv = getProcessVariable();

            // This will only be non-null upon second invocation
            DataSample<Double> lastKnownSignal = getLastKnownSignal();

            double error = getError();
            double p = error * getP();
            lastP = p;
            double signal = p;

            if (saturationLimit == 0) {
                double integral = getIntegral(lastKnownSignal, pv, error);
                lastI = integral * getI();
                signal += lastI;

            } else {

                if (lastKnownSignal != null && Math.abs(lastKnownSignal.sample) < saturationLimit) {
                    // Integral value will only be updated if the output
                    // is not saturated
                    double integral = getIntegral(lastKnownSignal, pv, error);
                    lastI = integral * getI();
                }

                signal += lastI;
            }

            double derivative = getDerivative(lastKnownSignal, pv, error);
            
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

                throw new IllegalStateException("signal is NaN, components: " + getStatus());
            }

            String sourceName = pv.sourceName + ".pc";
            String signature = MessageDigestCache.getMD5(sourceName).substring(0, 19);

            return new DataSample<Double>(pv.timestamp, sourceName, signature, signal, null);

        } finally {
            ThreadContext.pop();
        }
    }
    
    @Override
    protected final String getShortName() {
        return "pid";
    }

    @Override
    public final ProcessControllerStatus getStatus() {

        return new PidControllerStatus(getSetpoint(), getError(), getLastKnownSignal(), lastP, lastI, lastD);
    }

    protected abstract double getIntegral(DataSample<Double> lastKnownSignal, DataSample<Double>  pv, double error);
    protected abstract double getDerivative(DataSample<Double> lastKnownSignal, DataSample<Double>  pv, double error);
    
    public JmxDescriptor getJmxDescriptor() {
        return new JmxDescriptor(
                "dz",
                "PID Controller",
                jmxName,
                "Issue control signal based on P, I, D, Limit values");
    }

    @JmxAttribute(description = "Accumulated integral component")
    public final double getIntegral() {
        
        return lastI;
    }
    
    /**
     * @return {@link #resetOnSetpointChange}.
     */
    protected final boolean needResetOnSetpointChange() {
        
        return resetOnSetpointChange;
    }
    
    @JmxAttribute(description = "Whether to reset the accumulated integral component upon setpoint change")
    public boolean getResetOnSetpointChange() {
        
        return resetOnSetpointChange;
    }

    public void setResetOnSetpointChange(boolean reset) {
        
        this.resetOnSetpointChange = reset;
    }
}