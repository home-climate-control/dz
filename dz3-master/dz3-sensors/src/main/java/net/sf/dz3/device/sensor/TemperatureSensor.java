package net.sf.dz3.device.sensor;

import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.jmx.JmxAttribute;

/**
 * Dumb temperature sensor.
 *
 * <p>
 *
 * Though dumb, the temperature sensor is an <strong>active</strong> entity,
 * not passive: it produces the temperature change notifications that drive
 * the rest of the system.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko 2001-2010
 */
public interface TemperatureSensor extends AnalogSensor {

    /**
     * @return The current temperature in C.
     */
    @JmxAttribute(description = "Current temperature")
    DataSample<Double> getSignal();
}
