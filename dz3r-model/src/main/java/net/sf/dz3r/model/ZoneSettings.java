package net.sf.dz3r.model;

import net.sf.dz3r.signal.hvac.ZoneStatus;

/**
 * Zone settings.
 *
 * This object defines the desired zone configuration.
 *
 * @see ZoneStatus
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class ZoneSettings {

    public final Boolean enabled;
    public final Double setpoint;
    public final Boolean voting;
    public final Boolean hold;
    public final Integer dumpPriority;

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

    /**
     * Create an instance from a template, and enabled flag.
     *
     * @param template Template to copy all settings except {@link #enabled} from.
     * @param enabled {@link #enabled} flag to set.
     */
    public ZoneSettings(ZoneSettings template, boolean enabled) {
        this(enabled, template.setpoint, template.voting, template.hold, template.dumpPriority);
    }

    /**
     * Create an instance from a template, and a setpoint.
     *
     * @param template Template to copy all settings except {@link #enabled} from.
     * @param setpoint {@link #setpoint} to set.
     */
    public ZoneSettings(ZoneSettings template, Double setpoint) {
        this(template.enabled, setpoint, template.voting, template.hold, template.dumpPriority);
    }

    /**
     * Merge this instance with an update.
     *
     * @param from Adjustment instance. Non-null values take precedence over this object's values.
     */
    public ZoneSettings merge(ZoneSettings from) {

        return new ZoneSettings(
                from.enabled != null ? from.enabled : enabled,
                from.setpoint != null ? from.setpoint : setpoint,
                from.voting != null ? from.voting : voting,
                from.hold != null ? from.hold : hold,
                from.dumpPriority != null ? from.dumpPriority : dumpPriority);
    }

    public ZoneSettings(Boolean enabled, Double setpoint, Boolean voting, Boolean hold, Integer dumpPriority) {

        this.enabled = enabled;
        this.setpoint = setpoint;
        this.voting = voting;
        this.hold = hold;
        this.dumpPriority = dumpPriority;
    }

    @Override
    public String toString() {
        return "{enabled=" + enabled
                + ", setpoint=" + setpoint
                + ", voting=" + voting
                + ", hold=" + hold
                + ", dump=" + dumpPriority
                + "}";
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ZoneSettings
                && enabled == ((ZoneSettings) o).enabled
                && setpoint.equals(((ZoneSettings) o).setpoint)
                && voting == ((ZoneSettings) o).voting
                && hold == ((ZoneSettings) o).hold
                && dumpPriority.equals(((ZoneSettings) o).dumpPriority);
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }}
