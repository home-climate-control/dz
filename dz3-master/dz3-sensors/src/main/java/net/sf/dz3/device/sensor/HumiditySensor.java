package net.sf.dz3.device.sensor;

import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.jmx.JmxAttribute;

/**
 * Dumb humidity sensor.
 *
 * <p>
 *
 * Though dumb, the humidity sensor is an <strong>active</strong> entity,
 * not passive: it produces the humidity change notifications that drive
 * the rest of the system.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko 2001-2010
 */
public interface HumiditySensor extends AnalogSensor {

    /**
     * @return The current humidity.
     */
    @JmxAttribute(description = "Current humidity")
    DataSample<Double> getSignal();
}
