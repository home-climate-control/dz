package net.sf.dz3r.device.actuator.economizer;

import com.homeclimatecontrol.hcc.model.EconomizerSettings;
import com.homeclimatecontrol.hcc.model.HvacMode;
import net.sf.dz3r.controller.pid.PidController;

/**
 * Full set of economizer settings, both permanent and user changeable.
 *
 * @param mode Which mode this device is active in. This mode may be different from the zone mode; it is the user's
 * responsibility to make sure the system doesn't enter a runaway loop.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2026
 */
public record EconomizerConfig(HvacMode mode, Double P, Double I, Double saturationLimit, EconomizerSettings settings) {

    /**
     * All argument constructor.
     *
     * @param P Internal {@link PidController} P component.
     * @param I Internal {@link PidController} I component.
     * @param saturationLimit Internal {@link PidController} saturation limit.
     * @param settings User changeable settings.
     */
    public EconomizerConfig {

        if (mode == null) {
            throw new IllegalArgumentException("mode can't be null");
        }
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
