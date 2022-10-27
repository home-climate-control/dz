package net.sf.dz3r.device.actuator.economizer;

import net.sf.dz3r.model.HvacMode;

/**
 * Full set of economizer settings.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2022
 */
public class EconomizerSettings extends EconomizerTransientSettings {

    /**
     * Which mode this device is active in.
     */
    public final HvacMode mode;

    /**
     * {@code true} means that turning on the device will NOT turn the HVAC off.
     * You probably want to keep this at {@code false}, unless the indoor temperature is measured at HVAC return
     * and fresh air is injected into HVAC return.
     */
    public final boolean keepHvacOn;

    public final double P;

    public final double I;

    public final double saturationLimit;

    /**
     * Primary constructor with just the {@link #mode}, {@link #changeoverDelta}, and {@link #targetTemperature} values provided,
     * {@link #keepHvacOn} set to {@code false}, and PI controller with default settings.
     */
    public EconomizerSettings(HvacMode mode, Boolean enabled, double changeoverDelta, double targetTemperature) {
        this(mode, enabled, changeoverDelta, targetTemperature, false, 1, 0.000004, 1.1);
    }

    /**
     * All except {@code enabled} argument constructor (defaults to {@code true}, for common sense.
     *
     * @param keepHvacOn See {@link #keepHvacOn}. Think twice before setting this to {@code true}.
     * @param P Internal {@link net.sf.dz3r.controller.pid.PidController} P component.
     * @param I Internal {@link net.sf.dz3r.controller.pid.PidController} I component.
     * @param saturationLimit Internal {@link net.sf.dz3r.controller.pid.PidController} saturation limit.
     */
    public EconomizerSettings(HvacMode mode,
                              double changeoverDelta, double targetTemperature,
                              boolean keepHvacOn,
                              double P, double I, double saturationLimit) {
        this(mode, true, changeoverDelta, targetTemperature, keepHvacOn, P, I, saturationLimit);
    }

    /**
     * All argument constructor.
     *
     * @param keepHvacOn See {@link #keepHvacOn}. Think twice before setting this to {@code true}.
     * @param P Internal {@link net.sf.dz3r.controller.pid.PidController} P component.
     * @param I Internal {@link net.sf.dz3r.controller.pid.PidController} I component.
     * @param saturationLimit Internal {@link net.sf.dz3r.controller.pid.PidController} saturation limit.
     */
    public EconomizerSettings(HvacMode mode,
                              Boolean enabled,
                              double changeoverDelta, double targetTemperature,
                              boolean keepHvacOn,
                              double P, double I, double saturationLimit) {
        super(enabled, changeoverDelta, targetTemperature);

        this.mode = mode;
        this.keepHvacOn = keepHvacOn;

        this.P = P;
        this.I = I;
        this.saturationLimit = saturationLimit;

        checkArgs();
    }

    protected void checkArgs() {

        if (mode == null) {
            throw new IllegalArgumentException("mode can't be null");
        }

        if (changeoverDelta < 0) {
            throw new IllegalArgumentException("changeoverDelta must be non-negative");
        }
    }

    @Override
    public String toString() {
        return "{mode=" + mode
                + ", enabled=" + enabled
                + ", changeoverDelta=" + changeoverDelta
                + ", targetTemperature=" + targetTemperature
                + ", keepHvacOn=" + keepHvacOn
                + ", P=" + P
                + ", I=" + I
                + ", saturationLimit=" + saturationLimit
                + "}";
    }
}
