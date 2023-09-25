package net.sf.dz3r.signal.hvac;

import net.sf.dz3r.model.PeriodSettings;
import net.sf.dz3r.model.Zone;
import net.sf.dz3r.model.ZoneSettings;

/**
 * Zone status.
 *
 * This object defines the actual zone status in real time.
 *
 * @see net.sf.dz3r.model.ZoneSettings
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class ZoneStatus {

    public final ZoneSettings settings;
    public final CallingStatus callingStatus;
    public final EconomizerStatus economizerStatus;
    public final PeriodSettings periodSettings;

    /**
     * Create an instance.
     *
     * @param settings Zone settings derived from {@link Zone#settings}.
     * @param callingStatus Calling status as provided by the pipeline.
     * @param economizerStatus Economizer status as provided by the pipeline.
     * @param periodSettings Zone period settings derived from {@link Zone#periodSettings}.
     */
    public ZoneStatus(
            ZoneSettings settings,
            CallingStatus callingStatus,
            EconomizerStatus economizerStatus,
            PeriodSettings periodSettings) {

        this.settings = settings;
        this.callingStatus = callingStatus;
        this.economizerStatus = economizerStatus;
        this.periodSettings = periodSettings;
    }

    @Override
    public String toString() {
        return "{settings=" + settings + ", thermostat=" + callingStatus + ", economizer=" + economizerStatus + ", period=" + periodSettings + "}";
    }
}
