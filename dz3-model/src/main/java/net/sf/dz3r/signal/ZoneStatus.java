package net.sf.dz3r.signal;

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

    public ZoneStatus(ZoneSettings settings) {
        this.settings = settings;
    }
}
