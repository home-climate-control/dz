package net.sf.dz3.device.model;

import com.homeclimatecontrol.jukebox.jmx.JmxAttribute;

/**
 * Set of variables that define the desired zone status.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko</a> 2001-2009
 */
public interface ZoneStatus {

    @JmxAttribute(description = "Thermostat setpoint")
    double getSetpoint();

    /**
     * Is this thermostat allowed to initiate the A/C startup.
     *
     * If the thermostat is not allowed to initiate the A/C startup, then
     * the temperature in the room will be allowed to raise or drop
     * (depending on the mode) unrestricted. However, as soon as the A/C
     * unit is on, the temperature in the room will be brought to setpoint
     * exactly as for the voting room, and only after this the A/C will be
     * stopped.
     *
     * @return <code>true</code> if the thermostat is allowed to initiate the A/C
     * startup, <code>false</code> otherwise.
     */
    @JmxAttribute(description = "Is this zone currently voting")
    boolean isVoting();

    /**
     * Is this thermostat enabled.
     *
     * If the thermostat is not enabled, the dampers for the zone are closed
     *
     * @return <code>true</code> if it is enabled, <code>false</code>
     * otherwise.
     */
    @JmxAttribute(description = "Is this zone currently enabled")
    boolean isOn();

    /**
     * Get the dump priority.
     *
     * The dump priority determines the order in which the dampers will
     * close when the excessive static pressure has to be relieved.
     *
     * @return Dump priority.
     */
    @JmxAttribute(description = "In what order does this zone start acting as a dump zone (0 is never)")
    int getDumpPriority();
}