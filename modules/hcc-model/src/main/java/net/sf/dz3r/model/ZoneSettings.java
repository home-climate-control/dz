package net.sf.dz3r.model;

import net.sf.dz3r.device.actuator.economizer.EconomizerSettings;
import net.sf.dz3r.signal.hvac.ZoneStatus;

import java.util.Objects;

/**
 * Zone settings.
 *
 * This object defines the desired zone configuration.
 *
 * @see ZoneStatus
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2024
 */
public class ZoneSettings {

    public final Boolean enabled;
    public final Double setpoint;
    public final Boolean voting;
    public final Boolean hold;
    public final Integer dumpPriority;
    public final EconomizerSettings economizerSettings;

    /**
     * Create an instance from just a setpoint.
     *
     * Other values get reasonable defaults (enabled, voting, not on hold, no dump priority defined).
     *
     * @param setpoint Setpoint to set.
     */
    public ZoneSettings(double setpoint) {
        this(true, setpoint, true, false, 0, null);
    }

    /**
     * Create an instance from a template, and enabled flag.
     *
     * @param template Template to copy all settings except {@link #enabled} from.
     * @param enabled {@link #enabled} flag to set.
     */
    public ZoneSettings(ZoneSettings template, boolean enabled) {
        this(enabled, template.setpoint, template.voting, template.hold, template.dumpPriority, template.economizerSettings);
    }

    /**
     * Create an instance from a template, and a setpoint.
     *
     * @param template Template to copy all settings except {@link #enabled} from.
     * @param setpoint {@link #setpoint} to set.
     */
    public ZoneSettings(ZoneSettings template, Double setpoint) {
        this(template.enabled, setpoint, template.voting, template.hold, template.dumpPriority, template.economizerSettings);
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
                from.dumpPriority != null ? from.dumpPriority : dumpPriority,
                from.economizerSettings != null ? from.economizerSettings : economizerSettings);
    }

    /**
     * Abbreviated constructor without {@link #economizerSettings} to support incremental refactoring.
     *
     * @deprecated Use the full constructor instead.
     */
    @Deprecated
    public ZoneSettings(Boolean enabled, Double setpoint, Boolean voting, Boolean hold, Integer dumpPriority) {
        this(enabled, setpoint, voting, hold, dumpPriority, null);
    }

    /**
     * Full constructor.
     */
    public ZoneSettings(Boolean enabled, Double setpoint, Boolean voting, Boolean hold, Integer dumpPriority, EconomizerSettings economizerSettings) {

        this.enabled = enabled;
        this.setpoint = setpoint;
        this.voting = voting;
        this.hold = hold;
        this.dumpPriority = dumpPriority;
        this.economizerSettings = economizerSettings;
    }

    @Override
    public String toString() {
        return "{enabled=" + enabled
                + ", setpoint=" + setpoint
                + ", voting=" + voting
                + ", hold=" + hold
                + ", dump=" + dumpPriority
                + ", ecp=" + economizerSettings
                + "}";
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ZoneSettings other
                && enabled.equals(other.enabled)
                && setpoint.equals(other.setpoint)
                && voting.equals(other.voting)
                && ((hold == null && other.hold == null) || (hold != null && hold.equals(other.hold)))
                && dumpPriority.equals(other.dumpPriority)
                && ((economizerSettings == null && other.economizerSettings == null) || (economizerSettings != null && economizerSettings.equals(other.economizerSettings)));
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * @param other Settings to compare to. Must not be null.
     *
     * @return {@code true} if the user visible settings are the same (hold and dump priority are ignored).
     */
    public boolean same(ZoneSettings other) {

        if (other == null) {
            return false;
        }

        return Objects.equals(enabled, other.enabled)
                && setpoint.compareTo(other.setpoint) == 0
                && Objects.equals(voting, other.voting)
                && Objects.equals(economizerSettings, other.economizerSettings);
    }
}
