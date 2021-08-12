package net.sf.dz3r.signal;

import net.sf.dz3r.controller.ProcessController;
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
    public final boolean calling;
    public final double signal;

    public ZoneStatus(ZoneSettings settings, boolean calling, double signal) {
        this.settings = settings;
        this.calling = calling;
        this.signal = signal;
    }

    public ZoneStatus(ZoneSettings settings, ProcessController.Status<Double> status, ProcessController.Status<Double> payload) {
        this.settings = settings;
        this.calling = Double.compare(status.signal, 1d) == 0;
        this.signal = payload.signal;
    }

    @Override
    public String toString() {
        return "{settings=" + settings + ", calling=" + calling + ", signal=" + signal + "}";
    }
}
