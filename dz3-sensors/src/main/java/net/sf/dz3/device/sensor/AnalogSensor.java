package net.sf.dz3.device.sensor;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSource;
import com.homeclimatecontrol.jukebox.jmx.JmxAttribute;
import com.homeclimatecontrol.jukebox.jmx.JmxAware;

/**
 * Base interface for all analog sensors.
 * 
 * For now that would be just {@link TemperatureSensor} and {@link HumiditySensor}, later pressure sensor
 * and possibly others.
 * 
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2012
 */
public interface AnalogSensor  extends DataSource<Double>, JmxAware, Addressable {

    /**
     * @return The current signal.
     */
    @JmxAttribute(description = "Current signal")
    DataSample<Double> getSignal();
}
