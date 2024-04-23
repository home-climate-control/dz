package net.sf.dz3r.device.actuator.economizer;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Optional;

/**
 * Set of user changeable economizer settings.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2024
 */
@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public class EconomizerSettings {

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
    public final Boolean keepHvacOn;

    public final Double maxPower;

    public EconomizerSettings(double changeoverDelta, double targetTemperature, Boolean keepHvacOn, Double maxPower) {

        if (changeoverDelta < 0) {
            throw new IllegalArgumentException("changeoverDelta must be non-negative");
        }

        if (maxPower != null && (maxPower.isInfinite() || maxPower.isNaN() || maxPower <= 0 || maxPower > 1)) {
                throw new IllegalArgumentException("maxPower must be in range ]0,1]");
        }

        this.changeoverDelta = changeoverDelta;
        this.targetTemperature = targetTemperature;
        this.keepHvacOn = keepHvacOn;
        this.maxPower = maxPower;
    }

    public EconomizerSettings(EconomizerSettings source) {

        this.changeoverDelta = source.changeoverDelta;
        this.targetTemperature = source.targetTemperature;
        this.keepHvacOn = source.keepHvacOn;
        this.maxPower = source.maxPower;
    }

    @Override
    public String toString() {
        return "{changeoverDelta=" + changeoverDelta
                + ", targetTemperature=" + targetTemperature
                + ", keepHvacOn=" + keepHvacOn
                + ", maxPower=" + maxPower
                + "}";
    }

    public final boolean isKeepHvacOn() {
        return Optional.ofNullable(keepHvacOn).orElse(true);
    }

    public final double getMaxPower() {
        return Optional.ofNullable(maxPower).orElse(1d);
    }

    /**
     * Find out if the settings look the same to the user (doesn't imply they are {@link #equals(Object)}).
     *
     * @param other Settings to compare to.
     *
     * @return {@code true} if the user visible settings are the same.
     */
    public boolean same(EconomizerSettings other) {

        if (other == null) {
            return false;
        }

        return Double.compare(changeoverDelta, other.changeoverDelta) == 0
                && Double.compare(targetTemperature, other.targetTemperature) == 0
                && isKeepHvacOn() == other.isKeepHvacOn()
                && Double.compare(getMaxPower(), other.getMaxPower()) == 0;
    }
}
