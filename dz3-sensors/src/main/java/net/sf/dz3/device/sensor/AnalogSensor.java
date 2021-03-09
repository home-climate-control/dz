package net.sf.dz3.device.sensor;

import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.datastream.signal.model.DataSource;
import net.sf.jukebox.jmx.JmxAttribute;
import net.sf.jukebox.jmx.JmxAware;

/**
 * Base interface for all analog sensors.
 * 
 * For now that would be just {@link TemperatureSensor} and {@link HumiditySensor}, later pressure sensor
 * and possibly others.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko 2001-2012
 */
public interface AnalogSensor  extends DataSource<Double>, JmxAware, Addressable {

    /**
     * @return The current signal.
     */
    @JmxAttribute(description = "Current signal")
    DataSample<Double> getSignal();
}
