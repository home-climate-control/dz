package net.sf.dz3r.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import net.sf.dz3r.device.actuator.economizer.EconomizerSettings;
import net.sf.dz3r.signal.hvac.ZoneStatus;

import java.util.Optional;

/**
 * Zone settings.
 *
 * This object defines the desired zone configuration.
 *
 * @see ZoneStatus
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2024
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public class ZoneSettings {

    public final Boolean enabled;
    public final Double setpoint;
    public final Boolean voting;

    @JsonIgnore
    public final Boolean hold;

    public final Integer dumpPriority;

    @JsonProperty("economizer")
    public final EconomizerSettings economizerSettings;

    /**
     * Create an instance from just a setpoint.
     *
     * Other values get reasonable defaults (enabled, voting, not on hold, no dump priority defined).
     *
     * @param setpoint Setpoint to set.
     */
    public ZoneSettings(Double setpoint) {
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
                + ", economizer=" + economizerSettings
                + "}";
    }

    @JsonIgnore
    public boolean isEnabled() {
        return Optional.ofNullable(enabled).orElse(true);
    }

    @JsonIgnore
    public boolean isVoting() {
        return Optional.ofNullable(voting).orElse(true);
    }

    @JsonIgnore
    public boolean isOnHold() {
        return Optional.ofNullable(hold).orElse(false);
    }

    @JsonIgnore
    public int getDumpPriority() {
        return Optional.ofNullable(dumpPriority).orElse(0);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ZoneSettings other
                && isEnabled() == other.isEnabled()
                && setpoint.equals(other.setpoint)
                && isVoting() == other.isVoting()
                && ((hold == null && other.hold == null) || (hold != null && hold.equals(other.hold)))
                && getDumpPriority() == other.getDumpPriority()
                && ((economizerSettings == null && other.economizerSettings == null) || (economizerSettings != null && economizerSettings.equals(other.economizerSettings)));
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * Find out if the settings look the same to the user (doesn't imply they are {@link #equals(Object)}).
     *
     * @param other Settings to compare to.
     *
     * @return {@code true} if the user visible settings are the same (hold and dump priority are ignored).
     */
    public boolean same(ZoneSettings other) {

        if (other == null) {
            return false;
        }

        // Null values are interpreted as "true" for "enabled" and "voting"

        return (isEnabled() == other.isEnabled())
                && (setpoint.compareTo(other.setpoint) == 0)
                && (isVoting() == other.isVoting())
                && same(economizerSettings, other.economizerSettings);
    }

    private boolean same(EconomizerSettings a, EconomizerSettings b) {

        if (a == null && b == null) {
            return true;
        }

        if (b == null) {
            return false;
        }

        return Optional
                .ofNullable(a)
                .map(left -> left.same(b))
                .orElse(false);
    }
}
