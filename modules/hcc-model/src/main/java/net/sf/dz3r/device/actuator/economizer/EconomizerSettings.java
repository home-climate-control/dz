package net.sf.dz3r.device.actuator.economizer;

import net.sf.dz3r.model.HvacMode;

/**
 * Full set of economizer settings.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2022
 */
public class EconomizerSettings extends EconomizerTransientSettings {

    /**
     * Which mode this device is active in. This mode may be different from the zone mode; it is the user's
     * responsibility to make sure the system doesn't enter a runaway loop.
     */
    public final HvacMode mode;

    /**
     * {@code true} means that turning on the device will NOT turn the HVAC off.
     * You probably want to keep this at {@code false}, unless the indoor temperature is measured at HVAC return
     * and fresh air is injected into HVAC return.
     */
    public final Boolean keepHvacOn;

    public final Double P;

    public final Double I;

    public final Double saturationLimit;

    /**
     * All except {@code enabled} argument constructor (defaults to {@code true}, for common sense.
     *
     * @param keepHvacOn See {@link #keepHvacOn}. Think twice before setting this to {@code true}.
     * @param P Internal {@link net.sf.dz3r.controller.pid.PidController} P component.
     * @param I Internal {@link net.sf.dz3r.controller.pid.PidController} I component.
     * @param saturationLimit Internal {@link net.sf.dz3r.controller.pid.PidController} saturation limit.
     */
    public EconomizerSettings(HvacMode mode,
                              Double changeoverDelta, Double targetTemperature,
                              Boolean keepHvacOn,
                              Double P, Double I, Double saturationLimit) {
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
                              Double changeoverDelta, Double targetTemperature,
                              Boolean keepHvacOn,
                              Double P, Double I, Double saturationLimit) {
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

    /**
     * Merge this instance with an update.
     *
     * @param from Adjustment instance. Non-null values take precedence over this object's values.
     */
    public EconomizerSettings merge(EconomizerSettings from) {

        return new EconomizerSettings(
                from.mode != null ? from.mode : mode,
                from.enabled != null ? from.enabled : enabled,
                from.changeoverDelta != null ? from.changeoverDelta : changeoverDelta,
                from.targetTemperature != null ? from.targetTemperature : targetTemperature,
                from.keepHvacOn != null ? from.keepHvacOn : keepHvacOn,
                from.P != null ? from.P : P,
                from.I != null ? from.I : I,
                from.saturationLimit != null ? from.saturationLimit : saturationLimit);
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
