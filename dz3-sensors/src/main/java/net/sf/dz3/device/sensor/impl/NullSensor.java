package net.sf.dz3.device.sensor.impl;

import java.io.IOException;

import net.sf.dz3.device.sensor.TemperatureSensor;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;

/**
 * No-op {@link TemperatureSensor} implementation for the purpose of testing.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko</a> 2009
 */
public class NullSensor extends AbstractAnalogSensor {

    public NullSensor(String address, int pollInterval) {

        super(address, pollInterval);
    }

    @Override
    public DataSample<Double> getSensorSignal() throws IOException {
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    protected void shutdown() throws Throwable {
        
        // Do nothing
        
    }

    @Override
    protected void startup() throws Throwable {
        
        // Do nothing
    }
}
