package net.sf.dz3r.signal.hvac;

import net.sf.dz3r.model.ZoneSettings;

/**
 * Zone status.
 *
 * This object defines the actual zone status in real time.
 *
 * @see net.sf.dz3r.model.ZoneSettings
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class ZoneStatus {

    public final ZoneSettings settings;
    public final ThermostatStatus thermostatStatus;

    public ZoneStatus(ZoneSettings settings, ThermostatStatus thermostatStatus) {
        this.settings = settings;
        this.thermostatStatus = thermostatStatus;
    }

    @Override
    public String toString() {
        return "{settings=" + settings + ", thermostat=" + thermostatStatus + "}";
    }
}
