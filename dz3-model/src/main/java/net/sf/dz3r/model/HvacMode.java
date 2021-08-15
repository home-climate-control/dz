package net.sf.dz3r.model;

/**
 * HVAC device mode.
 *
 * Unlike the previous incarnation, this just includes actionable modes, {@code OFF} is handled by devices.
 *
 * @see net.sf.dz3.device.model.HvacMode
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public enum HvacMode {

    COOLING(-1, "Cooling"),
    HEATING(1, "Heating");

    public final int mode;
    public final String description;

    private HvacMode(int mode, String description) {
        this.mode = mode;
        this.description = description;
    }
}
