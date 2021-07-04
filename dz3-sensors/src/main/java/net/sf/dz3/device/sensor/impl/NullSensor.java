package net.sf.dz3.device.sensor.impl;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import net.sf.dz3.device.sensor.TemperatureSensor;

import java.io.IOException;

/**
 * No-op {@link TemperatureSensor} implementation for the purpose of testing.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2009-2021
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
