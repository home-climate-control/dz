package net.sf.dz3r.device.actuator.economizer;

import net.sf.dz3r.model.HvacMode;

public class PidEconomizerConfig extends EconomizerConfig {

    public final double P;

    public final double I;
    public final double saturationLimit;

    /**
     * Primary constructor with just the {@link #mode}, {@link #changeoverDelta}, and {@link #targetTemperature} values provided,
     * {@link #keepHvacOn} set to {@code false}, and PI controller with default settings.
     */
    public PidEconomizerConfig(HvacMode mode, double changeoverDelta, double targetTemperature) {

        // VT: FIXME: I and saturationLimit of 0 are bad defaults, will need to be adjusted when deployed to production
        this(mode, changeoverDelta, targetTemperature, false, 1, 0, 0);
    }

    /**
     * All argument constructor.
     *
     * @param keepHvacOn See {@link #keepHvacOn}. Think twice before setting this to {@code true}.
     * @param P Internal {@link net.sf.dz3r.controller.pid.PidController} P component.
     * @param I Internal {@link net.sf.dz3r.controller.pid.PidController} I component.
     * @param saturationLimit Internal {@link net.sf.dz3r.controller.pid.PidController} saturation limit.
     */
    public PidEconomizerConfig(HvacMode mode, double changeoverDelta, double targetTemperature,
                               boolean keepHvacOn,
                               double P, double I, double saturationLimit) {

        super(mode, changeoverDelta, targetTemperature, keepHvacOn);

        this.P = P;
        this.I = I;
        this.saturationLimit = saturationLimit;

        checkArgs();
    }

    @Override
    protected void checkArgs() {
        super.checkArgs();
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
