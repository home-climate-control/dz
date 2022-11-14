package net.sf.dz3r.signal.hvac;

import net.sf.dz3r.model.ZoneSettings;

/**
 * Zone status.
 *
 * This object defines the actual zone status in real time.
 *
 * @see net.sf.dz3r.model.ZoneSettings
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2022
 */
public class ZoneStatus {

    public final ZoneSettings settings;
    public final CallingStatus callingStatus;
    public final EconomizerStatus economizerStatus;

    public ZoneStatus(ZoneSettings settings, CallingStatus callingStatus, EconomizerStatus economizerStatus) {
        this.settings = settings;
        this.callingStatus = callingStatus;
        this.economizerStatus = economizerStatus;
    }

    @Override
    public String toString() {
        return "{settings=" + settings + ", thermostat=" + callingStatus + ", economizer=" + economizerStatus + "}";
    }
}
