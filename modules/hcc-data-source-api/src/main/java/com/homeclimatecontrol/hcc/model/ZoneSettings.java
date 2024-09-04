package com.homeclimatecontrol.hcc.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Optional;

/**
 * Runtime zone settings in a form that can be exposed to external systems.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2024
 */
@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record ZoneSettings(
        Boolean enabled,
        Double setpoint,
        Boolean voting,
        Boolean hold,
        Integer dumpPriority,
        @JsonProperty("economizer")
        EconomizerSettings economizerSettings
) {

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

    public boolean isEnabled() {
        return Optional.ofNullable(enabled).orElse(true);
    }

    public boolean isVoting() {
        return Optional.ofNullable(voting).orElse(true);
    }

    // Unless ignored, this will show up as a separate field in JSON representation
    @JsonIgnore
    public boolean isOnHold() {
        return Optional.ofNullable(hold).orElse(false);
    }

    public int getDumpPriority() {
        return Optional.ofNullable(dumpPriority).orElse(0);
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
