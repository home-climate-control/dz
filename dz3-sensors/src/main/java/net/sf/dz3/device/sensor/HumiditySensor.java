package net.sf.dz3.device.sensor;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.jmx.JmxAttribute;

/**
 * Dumb humidity sensor.
 *
 * <p>
 *
 * Though dumb, the humidity sensor is an <strong>active</strong> entity,
 * not passive: it produces the humidity change notifications that drive
 * the rest of the system.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2010
 */
public interface HumiditySensor extends AnalogSensor {

    /**
     * @return The current humidity.
     */
    @Override
    @JmxAttribute(description = "Current humidity")
    DataSample<Double> getSignal();
}
