package net.sf.dz3r.model;

/**
 * Zone settings.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class ZoneSettings {

    public final boolean enabled;
    public final double setpoint;
    public final boolean voting;
    public final boolean hold;
    public final int dumpPriority;

    public ZoneSettings(boolean enabled, double setpoint, boolean voting, boolean hold, int dumpPriority) {
        this.enabled = enabled;
        this.setpoint = setpoint;
        this.voting = voting;
        this.hold = hold;
        this.dumpPriority = dumpPriority;
    }
}
