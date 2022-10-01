package net.sf.dz3r.device.actuator.economizer;

import net.sf.dz3r.model.HvacMode;

public class EconomizerConfig {

    /**
     * Which mode this device is active in.
     */
    public final HvacMode mode;

    /**
     * Temperature difference between indoor and outdoor temperature necessary to turn the device on.
     *
     * VT: NOTE: Anti-jitter measures are provided elsewhere.
     */
    public final double changeoverDelta;

    /**
     * When this temperature is reached, the device is shut off.
     *
     * VT: NOTE: Anti-jitter measures are provided elsewhere.
     */
    public final double targetTemperature;

    /**
     * {@code true} means that turning on the device will NOT turn the HVAC off.
     * You probably want to keep this at {@code false}, unless the indoor temperature is measured at HVAC return
     * and fresh air is injected into HVAC return.
     */
    public final boolean keepHvacOn;

    /**
     * Primary constructor with just the {@link #mode}, {@link #changeoverDelta}, and {@link #targetTemperature} values provided,
     * and {@link #keepHvacOn} set to {@code false}.
     */
    public EconomizerConfig(HvacMode mode, double changeoverDelta, double targetTemperature) {
        this(mode, changeoverDelta, targetTemperature, false);
    }

    /**
     * All argument constructor.
     *
     * @param keepHvacOn See {@link #keepHvacOn}. Think twice before setting this to {@code true}.
     */
    public EconomizerConfig(HvacMode mode, double changeoverDelta, double targetTemperature, boolean keepHvacOn) {
        this.mode = mode;
        this.changeoverDelta = changeoverDelta;
        this.targetTemperature = targetTemperature;
        this.keepHvacOn = keepHvacOn;
    }
}
