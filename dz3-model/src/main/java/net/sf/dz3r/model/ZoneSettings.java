package net.sf.dz3r.model;

/**
 * Zone settings.
 *
 * This object defines the desired zone configuration.
 *
 * @see net.sf.dz3r.signal.ZoneStatus
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class ZoneSettings {

    public final boolean enabled;
    public final double setpoint;
    public final boolean voting;
    public final boolean hold;
    public final int dumpPriority;

    /**
     * Create an instance from just a setpoint.
     *
     * Other values get reasonable defaults (enabled, voting, not on hold, no dump priority defined).
     *
     * @param setpoint Setpoint to set.
     */
    public ZoneSettings(double setpoint) {
        this(true, setpoint, true, false, 0);
    }

    public ZoneSettings(boolean enabled, double setpoint, boolean voting, boolean hold, int dumpPriority) {
        this.enabled = enabled;
        this.setpoint = setpoint;
        this.voting = voting;
        this.hold = hold;
        this.dumpPriority = dumpPriority;
    }
}
