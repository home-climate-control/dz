package com.homeclimatecontrol.hcc.signal.hvac;

import com.homeclimatecontrol.hcc.model.EconomizerSettings;
import com.homeclimatecontrol.hcc.signal.Signal;

/**
 * Economiser status.
 *
 * @see net.sf.dz3r.device.actuator.economizer.AbstractEconomizer
 * @see net.sf.dz3r.signal.health.EconomizerStatus
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2024
 */
public record EconomizerStatus(
        EconomizerSettings settings,
        CallingStatus callingStatus,
        Signal<Double, Void> ambient
) {

    public EconomizerStatus(EconomizerSettings settings, Double sample, double demand, boolean calling, Signal<Double, Void> ambient) {
        this(settings, new CallingStatus(sample, demand, calling), ambient);
    }
}
