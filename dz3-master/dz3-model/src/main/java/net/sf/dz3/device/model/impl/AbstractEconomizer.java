package net.sf.dz3.device.model.impl;

import net.sf.dz3.controller.pid.AbstractPidController;
import net.sf.dz3.device.model.Economizer;
import net.sf.dz3.device.sensor.AnalogSensor;
import net.sf.dz3.util.digest.MessageDigestCache;
import net.sf.jukebox.datastream.logger.impl.DataBroadcaster;
import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.datastream.signal.model.DataSink;
import net.sf.jukebox.jmx.JmxAttribute;
import net.sf.jukebox.jmx.JmxAware;

import org.apache.log4j.Logger;

/**
 * Base class for {@link Economizer} implementations.
 *
 * Handles common inputs and outputs.
 *  
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2010
 */
public abstract class AbstractEconomizer implements Economizer, JmxAware {

    protected final Logger logger = Logger.getLogger(getClass());
    private final DataBroadcaster<Double> dataBroadcaster = new DataBroadcaster<Double>();
    
    protected final String name;
    protected final String signature;
    
    protected final ThermostatModel thermostat;
    protected final AnalogSensor outdoorSensor;
    /**
     * @see #getThreshold()
     */
    protected double threshold;
    
    /**
     * Last known indoor temperature - or {@code null} if unknown.
     */
    protected Double indoor;
    
    /**
     * Last known outdoor temperature - or {@code null} if unknown.
     */
    protected Double outdoor;
    
    /**
     * Timestamp for last known indoor or outdoor temperature values.
     */
    protected long timestamp;
    
    /**
     * Last issued signal.
     */
    private DataSample<Double> signal;

    public AbstractEconomizer(String name, ThermostatModel thermostat, AnalogSensor outdoorSensor, double threshold) {
        
        this.name = name;
        this.signature = MessageDigestCache.getMD5(name).substring(0, 19);
        
        this.thermostat = thermostat;
        this.outdoorSensor = outdoorSensor;
        
        init();
    }
    
    /**
     * Initialize the instance.
     */
    abstract protected void init();

    @Override
    public final double getThreshold() {
        
        return threshold;
    }

    public final void setThreshold(double threshold) {
        
        if (threshold < 0) {
            throw new IllegalArgumentException("threshold value must be non-negative (" + threshold + " was given)");
        }
        
        this.threshold = threshold;
        
    }

    @Override
    public final void addConsumer(DataSink<Double> consumer) {
        
        dataBroadcaster.addConsumer(consumer);
    }

    @Override
    public final void removeConsumer(DataSink<Double> consumer) {
        
        dataBroadcaster.removeConsumer(consumer);
    }

    /**
     * @return {@code true} if the {@link #thermostat} is in cooling mode.
     */
    protected final boolean isCooling() {
        
        return ((AbstractPidController) thermostat.getController()).getP() > 0;
    }

    /**
     * Broadcast the signal to listeners.
     * 
     * @param signal Signal to broadcast.
     * @see #compute()
     */
    protected final void broadcast(double signal) {
        
        this.signal = new DataSample<Double>(timestamp, name, signature, signal, null);
        
        dataBroadcaster.broadcast(this.signal);
    }
    
    @JmxAttribute(description = "Last issued signal")
    public DataSample<Double> getSignal() {
        
        return signal;
    }

    /**
     * Compute the output signal.
     * 
     * @return Output signal computed. Positive value means the device must be turned on.
     * 
     * By design contract, value returned must not be negative.
     */
    abstract protected double compute();
}