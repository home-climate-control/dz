package net.sf.dz3r.device.actuator.economizer;

import net.sf.dz3r.model.HvacMode;

public class EconomizerSettings {

    public final String name;

    /**
     * Which mode this device is active in.
     */
    public final HvacMode mode;

    /**
     * Temperature difference between indoor and outdoor temperature necessary to turn the device on.
     */
    public final double changeoverDelta;

    /**
     * When this temperature is reached, the device is shut off.
     */
    public final double targetTemperature;

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
    public EconomizerSettings( String name, HvacMode mode, double changeoverDelta, double targetTemperature) {
        // VT: FIXME: I and saturationLimit of 0 are bad defaults, will need to be adjusted when deployed to production
        this(name, mode, changeoverDelta, targetTemperature, false, 1, 0, 0);
    }

    /**
     * All argument constructor.
     *
     * @param keepHvacOn See {@link #keepHvacOn}. Think twice before setting this to {@code true}.
     * @param P Internal {@link net.sf.dz3r.controller.pid.PidController} P component.
     * @param I Internal {@link net.sf.dz3r.controller.pid.PidController} I component.
     * @param saturationLimit Internal {@link net.sf.dz3r.controller.pid.PidController} saturation limit.
     */
    public EconomizerSettings(String name,
                              HvacMode mode, double changeoverDelta, double targetTemperature,
                              boolean keepHvacOn,
                              double P, double I, double saturationLimit) {
        this.name = name;

        this.mode = mode;
        this.changeoverDelta = changeoverDelta;
        this.targetTemperature = targetTemperature;
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
                + ", changeoverDelta=" + changeoverDelta
                + ", targetTemperature=" + targetTemperature
                + ", keepHvacOn=" + keepHvacOn
                + ", P=" + P
                + ", I=" + I
                + ", saturationLimit=" + saturationLimit
                + "}";
    }
}
