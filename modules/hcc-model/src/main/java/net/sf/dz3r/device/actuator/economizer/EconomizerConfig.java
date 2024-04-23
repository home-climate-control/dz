package net.sf.dz3r.device.actuator.economizer;

import net.sf.dz3r.model.HvacMode;

/**
 * Full set of economizer settings, both permanent and user changeable.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2024
 */
public class EconomizerConfig {

    /**
     * Which mode this device is active in. This mode may be different from the zone mode; it is the user's
     * responsibility to make sure the system doesn't enter a runaway loop.
     */
    public final HvacMode mode;

    public final Double P;

    public final Double I;

    public final Double saturationLimit;

    public final EconomizerSettings settings;

    /**
     * All argument constructor.
     *
     * @param P Internal {@link net.sf.dz3r.controller.pid.PidController} P component.
     * @param I Internal {@link net.sf.dz3r.controller.pid.PidController} I component.
     * @param saturationLimit Internal {@link net.sf.dz3r.controller.pid.PidController} saturation limit.
     * @param settings User changeable settings.
     */
    public EconomizerConfig(HvacMode mode,
                            Double P, Double I, Double saturationLimit,
                            EconomizerSettings settings) {

        if (mode == null) {
            throw new IllegalArgumentException("mode can't be null");
        }

        this.mode = mode;

        this.P = P;
        this.I = I;
        this.saturationLimit = saturationLimit;

        this.settings = settings;
    }

    /**
     * Merge this instance with an update.
     *
     * @param from Adjustment instance. Non-null values take precedence over this object's values.
     */
    public EconomizerConfig merge(EconomizerConfig from) {

        return new EconomizerConfig(
                from.mode,
                from.P != null ? from.P : P,
                from.I != null ? from.I : I,
                from.saturationLimit != null ? from.saturationLimit : saturationLimit,
                from.settings != null ? from.settings : settings);
    }

    @Override
    public String toString() {
        return "{mode=" + mode
                + ", enabled=" + isEnabled()
                + ", P=" + P
                + ", I=" + I
                + ", saturationLimit=" + saturationLimit
                + ", settings=" + settings
                + "}";
    }

    public boolean isEnabled() {
        return settings != null;
    }

    public EconomizerConfig merge(EconomizerSettings settings) {

        return new EconomizerConfig(
                mode,
                P,
                I,
                saturationLimit,
                settings);
    }
}
