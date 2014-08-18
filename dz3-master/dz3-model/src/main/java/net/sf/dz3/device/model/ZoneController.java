package net.sf.dz3.device.model;

import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.datastream.signal.model.DataSink;
import net.sf.jukebox.datastream.signal.model.DataSource;
import net.sf.jukebox.jmx.JmxAttribute;
import net.sf.jukebox.jmx.JmxAware;

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
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2012
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
