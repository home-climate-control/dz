package net.sf.dz3.device.model;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSource;
import com.homeclimatecontrol.jukebox.jmx.JmxAttribute;
import com.homeclimatecontrol.jukebox.jmx.JmxAware;

/**
 * Zone controller.
 * 
 * Accepts the signals from the thermostats and issues control signals to the {@link Unit}
 * and {@link DamperController}.
 * 
 * Zone controller is {@link HvacMode} agnostic, all it does is issues signals related to demand,
 * its {@link #getSignal() signal} is never negative.
 * 
 * To take HVAC mode into account, thermostat settings need to be adjusted.
 * 
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2012
 */
public interface ZoneController extends DataSink<ThermostatSignal>, DataSource<Double>, JmxAware {

    /**
     * Get the last signal issued.
     * 
     * @return Last output signal.
     */
    @JmxAttribute(description = "Last output signal")
    public DataSample<Double> getSignal();
}
