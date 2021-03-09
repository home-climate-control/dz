package net.sf.dz3.device.model;

import net.sf.jukebox.jmx.JmxAttribute;

/**
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko</a> 2001-2009
 */
public interface ThermostatStatus extends ZoneStatus {
    
    /**
     * Is this thermostat requested to hold the temperature regardless of
     * the scheduled changes.
     *
     * @return {@code true} if this thermostat is on hold,
     * {@code false} otherwise.
     */
    @JmxAttribute(description = "Is this zone currently on hold")
    boolean isOnHold();

    /**
     * Get the control signal value.
     *
     * <p>
     *
     * <strong>NOTE:</strong> The value returned here is different from the
     * value returned by the {@link #getController controller} - it is
     * adjusted based on whether this thermostat is {@link #isOn enabled} or
     * not.
     *
     * @return If {@link #isOn enabled}, the value of
     * the control signal, otherwise 0.
     */
    @JmxAttribute(description = "Control signal value")
    double getControlSignal();
    
    /**
     * Check whether the sensor for this thermostat is faulty.
     *  
     * @return {@code true} if the last reported sensor signal is {@link DataSample#error an error}.
     */
    @JmxAttribute(description = "Whether the sensor for this thermostat is faulty")
    boolean isError();
}
