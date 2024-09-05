package com.homeclimatecontrol.hcc.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Optional;

/**
 * Runtime economizer settings in a form that can be exposed to external systems.
 *
 * @param changeoverDelta Temperature difference between indoor and outdoor temperature necessary to turn the device on.
 * @param targetTemperature When this temperature is reached, the device is shut off.
 * @param keepHvacOn {@code true} means that turning on the device will NOT turn the HVAC off.
 *   You probably want to keep this at {@code false}, unless the indoor temperature is measured at HVAC return
 *   and fresh air is injected into HVAC return.
 * @param maxPower Max power to deliver to the HVAC unit when the economizer is on; 1 is full, 0 is off (not very useful).
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2024
 */
@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record EconomizerSettings(
        double changeoverDelta,
        double targetTemperature,
        Boolean keepHvacOn,
        Double maxPower
) {

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
        this(source.changeoverDelta(), source.targetTemperature(), source.keepHvacOn(), source.maxPower());
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
