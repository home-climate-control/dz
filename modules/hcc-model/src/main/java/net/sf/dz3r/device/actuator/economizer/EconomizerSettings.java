package net.sf.dz3r.device.actuator.economizer;

/**
 * Set of user changeable economizer settings.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2024
 */
public class EconomizerSettings {

    public final Boolean enabled;

    /**
     * Temperature difference between indoor and outdoor temperature necessary to turn the device on.
     */
    public final Double changeoverDelta;

    /**
     * When this temperature is reached, the device is shut off.
     */
    public final Double targetTemperature;

    /**
     * {@code true} means that turning on the device will NOT turn the HVAC off.
     * You probably want to keep this at {@code false}, unless the indoor temperature is measured at HVAC return
     * and fresh air is injected into HVAC return.
     */
    public final Boolean keepHvacOn;

    public EconomizerSettings(Boolean enabled, Double changeoverDelta, Double targetTemperature, Boolean keepHvacOn) {

        this.enabled = enabled;
        this.changeoverDelta = changeoverDelta;
        this.targetTemperature = targetTemperature;
        this.keepHvacOn = keepHvacOn;
    }

    public EconomizerSettings(EconomizerConfig source) {

        this.enabled = source.enabled;
        this.changeoverDelta = source.changeoverDelta;
        this.targetTemperature = source.targetTemperature;
        this.keepHvacOn = source.keepHvacOn;
    }

    @Override
    public String toString() {
        return "{enabled=" + enabled
                + ", changeoverDelta=" + changeoverDelta
                + ", targetTemperature=" + targetTemperature
                + ", keepHvacOn=" + keepHvacOn
                + "}";
    }
}
