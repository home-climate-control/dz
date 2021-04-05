package net.sf.dz3.device.model;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSource;
import com.homeclimatecontrol.jukebox.jmx.JmxAttribute;
import com.homeclimatecontrol.jukebox.jmx.JmxAware;

/**
 * The virtual thermostat.
 *
 * <p>
 *
 * Takes the notification from the {@link #getSensor() temperature sensor}, and
 * if it is {@link #isOn enabled}, passes it to the {@link #getController()
 * controller}. The thermostat may not be {@link #isVoting() voting}, in
 * this case the {@link ZoneController zone controller} will not start the
 * A/C if just this thermostat is unhappy, however, it will not shut it down
 * until it is happy once it is started.
 *
 * <p>
 *
 * Note that this interface doesn't have mutator methods. To control the
 * thermostat implementation state and behavior, use {@link
 * ThermostatController ThermostatController}.
 *
 * <p>
 *
 * It is an interface to allow the adapters to be implemented - in
 * particular, for remote access.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2012
 */
public interface Thermostat extends ThermostatStatus,
	DataSink<Double>,
	DataSource<ThermostatSignal>,
	Comparable<Thermostat>,
	JmxAware {

    /**
     * Get the thermostat name.
     *
     * @return Human readable thermostat name.
     */
    @JmxAttribute(description = "Zone name")
    String getName();
    
    /**
     * Get the last signal issued.
     * 
     * @return Last output signal.
     */
    @JmxAttribute(description = "Last output signal")
    ThermostatSignal getSignal();
 
    /**
     * Get the current setpoint.
     * 
     * @return Thermostat's setpoint.
     */
    @JmxAttribute(description = "Setpoint")
    double getSetpoint();

    /**
     * Make the thermostat reconsider its calling status.
     * 
     * If it is calling, there must be no change.
     * If it is not calling, but the signal is within the hysteresis loop, status must change to calling.
     * If if it not calling, and the signal is below the hysteresis loop, there must be no change.
     */
    void raise();

    
    /**
     * Set the zone status to the given status, unless the zone is {@link ThermostatStatus#isOnHold() on hold}.
     * 
     * @param status Status to set.
     */
    void set(ZoneStatus status);
}
