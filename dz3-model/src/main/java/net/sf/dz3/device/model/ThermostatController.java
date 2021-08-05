package net.sf.dz3.device.model;

/**
 * The virtual thermostat controller.
 *
 * <p>
 *
 * It is an interface to allow the adapters to be implemented - in
 * particular, for remote access.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2009
 */
public interface ThermostatController  {

    /**
     * Enable or disable this thermostat.
     *
     * If the thermostat is disabled, the zone it controls is considered
     * shut off - it will always issue a "happy" signal. However, it will
     * participate in excessive pressure relief, but this has nothing to do
     * with the thermostat per se - it is controlled by the damper
     * controller.
     *
     * @param enabled Enabled state. {@code false} means the zone controlled by this thermostat is shut off.
     */
    void setOn(boolean enabled);

    /**
     * Put this thermostat on hold.
     *
     * If the thermostat is on hold, the scheduler will not change the
     * setpoint for it when the new period starts. Actually, this is rather
     * an informative - the scheduler will make the decision, however,
     * logically, it belongs to the thermostat controller, so it was placed
     * here.
     *
     * @param hold Hold state. {@code true} means the thermostat is on hold.
     */
    void setOnHold(boolean hold);

    /**
     * Make this thermostat voting or non-voting.
     *
     * If the thermostat is not voting, it will not cause the HVAC unit to
     * be switched on. However, the HVAC unit will not be shut off until
     * this zone is satisfied.
     *
     * @param voting {@code true} if this thermostat's zone is voting, {code @false otherwise}.
     */
    void setVoting(boolean voting);

    void setDumpPriority(int dumpPriority);

    void setSetpoint(double setpoint);
}
