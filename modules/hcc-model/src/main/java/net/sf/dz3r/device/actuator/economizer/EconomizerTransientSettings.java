package net.sf.dz3r.device.actuator.economizer;

/**
 * Set of economizer settings that can change over time by the user.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2022
 */
public class EconomizerTransientSettings {

    public final Boolean enabled;

    /**
     * Temperature difference between indoor and outdoor temperature necessary to turn the device on.
     */
    public final Double changeoverDelta;

    /**
     * When this temperature is reached, the device is shut off.
     */
    public final Double targetTemperature;

    public EconomizerTransientSettings(Boolean enabled, Double changeoverDelta, Double targetTemperature) {

        this.enabled = enabled;
        this.changeoverDelta = changeoverDelta;
        this.targetTemperature = targetTemperature;
    }

    public EconomizerTransientSettings(EconomizerSettings source) {

        this.enabled = source.enabled;
        this.changeoverDelta = source.changeoverDelta;
        this.targetTemperature = source.targetTemperature;
    }

    @Override
    public String toString() {
        return "{enabled=" + enabled
                + ", changeoverDelta=" + changeoverDelta
                + ", targetTemperature=" + targetTemperature
                + "}";
    }
}
