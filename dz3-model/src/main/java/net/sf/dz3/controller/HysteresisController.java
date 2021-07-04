package net.sf.dz3.controller;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.jmx.JmxAttribute;
import net.sf.dz3.util.digest.MessageDigestCache;

/**
 * A hysteresis controller.
 * <p>
 * The controller output becomes positive when the process variable becomes
 * higher than the setpoint plus hysteresis, and it becomes negative when the
 * process variable becomes lower than the setpoint minus hysteresis.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2009
 */
public class HysteresisController extends AbstractProcessController {

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
    public HysteresisController(double setpoint) {

	this(setpoint, -1f, 1f);
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
    public final synchronized DataSample<Double> compute() {

	DataSample<Double> pv = getProcessVariable();

	if (pv == null || pv.isError()) {

	    // Don't have to do a thing, nothing happened yet, nothing good at least
	    return null;

	} else {

	    double setpoint = getSetpoint();

	    if (state) {

		if (pv.sample - thresholdLow <= setpoint) {

		    state = false;
		}

	    } else {

		if (pv.sample - thresholdHigh >= setpoint) {

		    state = true;
		}
	    }
	}

        String sourceName = pv.sourceName + ".pc";
        String signature = MessageDigestCache.getMD5(sourceName).substring(0, 19);

        return new DataSample<Double>(pv.timestamp, sourceName, signature,state ? 1.0 : -1.0, null);
    }

    /**
     * @return Controller {@link #state}.
     */
    @JmxAttribute(description = "state")
    public boolean getState() {

	return state;
    }

    @Override
    protected final String getShortName() {
        return "hys";
    }

    @Override
    public ProcessControllerStatus getStatus() {

	ProcessControllerStatus superStatus = super.getStatus();

        return new HysteresisControllerStatus(superStatus, state);
    }

    @Override
    protected void setpointChanged() {

        // Do absolutely nothing.
    }
}
