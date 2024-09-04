package net.sf.dz3r.signal.hvac;

import com.homeclimatecontrol.hcc.model.EconomizerSettings;
import net.sf.dz3r.signal.Signal;

/**
 * Economiser status.
 *
 * @see net.sf.dz3r.device.actuator.economizer.AbstractEconomizer
 * @see net.sf.dz3r.signal.health.EconomizerStatus
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class EconomizerStatus {

    public final EconomizerSettings settings;
    public final CallingStatus callingStatus;
    public final Signal<Double, Void> ambient;

    public EconomizerStatus(EconomizerSettings settings, Double sample, double demand, boolean calling, Signal<Double, Void> ambient) {

        this.settings = settings;
        this.callingStatus = new CallingStatus(sample, demand, calling);
        this.ambient = ambient;
    }

    @Override
    public String toString() {
        return "{demand=" + callingStatus.demand + ", calling=" + callingStatus.calling + ", ambient=" + ambient + ", settings=" + settings + "}";
    }
}
