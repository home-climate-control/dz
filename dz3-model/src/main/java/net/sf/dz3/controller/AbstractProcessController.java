package net.sf.dz3.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import net.sf.dz3.util.digest.MessageDigestCache;
import com.homeclimatecontrol.jukebox.datastream.logger.impl.DataBroadcaster;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;

/**
 * An abstract process controller.
 *
 * Implements the functionality common for different kinds of process controllers.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko</a> 2001-2009
 */
public abstract class AbstractProcessController implements ProcessController {
    
    protected final Logger logger = LogManager.getLogger(getClass());
    
    /**
     * Process controller signal broadcaster.
     */
    private final DataBroadcaster<ProcessControllerStatus> dataBroadcaster = new DataBroadcaster<ProcessControllerStatus>();

    /**
     * The process setpoint.
     */
    private double setpoint;

    /**
     * The current process variable value.
     */
    private DataSample<Double> pv;

    /**
     * Last known signal. This is used to support the listener notification.
     */
    private DataSample<Double> lastKnownSignal = null;
    
    /**
     * Create an instance.
     * 
     * @param setpoint Initial setpoint.
     */
    public AbstractProcessController(double setpoint) {
	
	this.setpoint = setpoint;
    }

    /**
     * Get last known signal value.
     *
     * @return Last known signal value, or <code>null</code> if it is not yet
     * available.
     */
    protected final DataSample<Double> getLastKnownSignal() {

        return lastKnownSignal;
    }

    @Override
    public final void setSetpoint(final double setpoint) {

        this.setpoint = setpoint;

        setpointChanged();
        statusChanged();

        wrapCompute();
    }

    /**
     * Perform any actions necessary for the controller to handle the setpoint change
     * (other than actually changing the setpoint). 
     */
    abstract protected void setpointChanged();

    @Override
    public final double getSetpoint() {

        return setpoint;
    }

    /**
     * Get the process variable.
     *
     * @return The process variable.
     */
    public final DataSample<Double> getProcessVariable() {

        return pv;
    }

    @Override
    public final synchronized double getError() {
	
	if (pv == null) {
	    
	    // No sample, no error
	    return 0;
	}

        return pv.sample - setpoint;
    }

    @Override
    public final synchronized DataSample<Double> compute(DataSample<Double> pv) {
	
	if (pv == null) {
	    throw new IllegalArgumentException("pv can't be null");
	}
	
	if (pv.isError()) {
	    throw new IllegalArgumentException("pv can't be an error");
	}
	
	if (lastKnownSignal != null) {
	    long then = lastKnownSignal.timestamp;
	    long now = pv.timestamp;
	    long diff = now - then;
	    
	    if (diff <= 0) {
		throw new IllegalArgumentException("Can't go back in time: last sample was @"
			+ then + ", this is @" + now + ", " + diff + "ms difference");
	    }
	}
	
        this.pv = pv;

        return wrapCompute();
    }

    @Override
    public void consume(DataSample<Double> sample) {
        
        compute(sample);
    }

    /**
     * Wrap the {@link #compute(long, double) compute()} method to support the
     * listeners without incurring complexity on the subclasses.
     *
     * @return Result of {@link #compute()}.
     */
    private DataSample<Double> wrapCompute() {

	ThreadContext.push("wrapCompute");

        try {

            lastKnownSignal = compute();

            // This will not throw anything
            statusChanged();
            
            return lastKnownSignal;

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Compute the controller output based on the known state.
     *
     * @return controller output.
     */
    protected abstract DataSample<Double> compute();

    @Override
    public void addConsumer(DataSink<ProcessControllerStatus> consumer) {
        dataBroadcaster.addConsumer(consumer);
    }

    @Override
    public void removeConsumer(DataSink<ProcessControllerStatus> consumer) {
        dataBroadcaster.removeConsumer(consumer);
    }

    /**
     * Send a notification to {@link #listenerSet listeners} about the status
     * change.
     */
    protected synchronized final void statusChanged() {

        if (lastKnownSignal == null) {
            
            // It is happening at instantiation
            return;
        }

        ProcessControllerStatus status = getStatus();
            
	// VT: NOTE: This will not be an error signal even if the original signal is,
        // the purpose is not control but instrumentation
        
        String sourceName = lastKnownSignal.sourceName + "." + getShortName();
        String signature = MessageDigestCache.getMD5(sourceName).substring(0, 19);

        DataSample<ProcessControllerStatus> sample = new DataSample<ProcessControllerStatus>(lastKnownSignal.timestamp,
                sourceName, signature, status, null);
        
        dataBroadcaster.broadcast(sample);
    }

    /**
     * @return A short name to add to the instrumentation data sample source name, to distinguish it from
     * the actual source signal.
     * 
     */
    abstract protected String getShortName();

    /**
     * Get the extended process controller status. This implementation is
     * provided for lazy programmers and returns only the information that is
     * available for all the subclasses - the last known output signal, if it is
     * available, or string "NaN", if not.
     *
     * @return Status object.
     */
    @Override
    public ProcessControllerStatus getStatus() {

        return new ProcessControllerStatus(
        	getSetpoint(),
        	getError(),
        	lastKnownSignal);
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();

        sb.append(getShortName()).append("(").append(getStatus()).append(")");

        return sb.toString();
    }
}
