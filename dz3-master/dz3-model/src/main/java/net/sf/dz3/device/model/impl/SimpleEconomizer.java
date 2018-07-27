package net.sf.dz3.device.model.impl;

import org.apache.logging.log4j.ThreadContext;

import net.sf.dz3.device.model.Economizer;
import net.sf.dz3.device.sensor.AnalogSensor;
import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.datastream.signal.model.DataSink;
import net.sf.jukebox.jmx.JmxDescriptor;

/**
 * Simple {@link Economizer} implementation.
 * 
 *  This is a proof of concept implementation, not protected from the jitter possible when
 *  temperature readings fluctuate around triggering point.
 *  
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2018
 */
public class SimpleEconomizer extends AbstractEconomizer {
    
    public SimpleEconomizer(String name, ThermostatModel thermostat, AnalogSensor outdoorSensor, double threshold) {

        super(name, thermostat, outdoorSensor, threshold);
    }
    
    protected void init() {
        
        thermostat.getSensor().addConsumer(new ThermostatListener());
        outdoorSensor.addConsumer(new SensorListener());
    }

    @Override
    protected double compute() {
        
        ThreadContext.push("compute");
        
        try {
            
            logger.debug("Indoor temperature: " + indoor);
            logger.debug("Outdoor temperature: " + outdoor);
            
            if (indoor == null || outdoor == null) {

                // Must've been an error, or just starting up
                logger.debug("data unavailable, ignored");
                
                return 0;
            }
            
            if (!thermostat.isOn()) {
                
                logger.debug("off, ignored");
                return 0;
            }
            
            if (!thermostat.isVoting()) {
                
                logger.debug("not voting, ignored");
                return 0;
            }

            double setpoint = thermostat.getSetpoint();
            
            if (isCooling()) {
             
                if (indoor < setpoint) {
                    
                    // No cooling required
                    return 0;
                }
                
                double signal = indoor - threshold - outdoor;
                
                return signal > 0 ? signal : 0;
                
            } else {
                
                if (indoor > setpoint) {
                    
                    // No heating required
                    return 0;
                }
                
                double signal = outdoor - threshold - indoor;
                
                return signal > 0 ? signal : 0;
            }

        } finally {
            ThreadContext.pop();
        }
    }

    class ThermostatListener implements DataSink<Double> {

        @Override
        public void consume(DataSample<Double> signal) {
            
            timestamp = signal.timestamp;
            indoor = signal.sample;
            
            broadcast(compute());
        }
    }

    class SensorListener implements DataSink<Double> {

        @Override
        public void consume(DataSample<Double> signal) {

            timestamp = signal.timestamp;
            outdoor = signal.sample;
            
            broadcast(compute());
        }
    }

    @Override
    public JmxDescriptor getJmxDescriptor() {

        return new JmxDescriptor(
                "dz",
                "Simple Economizer",
                Integer.toHexString(hashCode()),
                "Utilize indoor/outdoor termperature difference to minimize energy consumption");
    }
}
