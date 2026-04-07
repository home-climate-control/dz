package com.homeclimatecontrol.hcc.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Optional;

/**
 * Runtime economizer settings in a form that can be exposed to external systems.
 *
 * @param changeoverDelta Temperature difference between indoor and outdoor temperature necessary to turn the device on.
 * @param targetTemperature When this temperature is reached, the device is shut off.
 * @param hvacHandoffFactor HVAC demand threshold at which the HVAC is turned on regardless of whether the economizer is on.
 *   When {@code null}, the economizer suppresses the HVAC entirely while active.
 *   A value of {@code 0} means HVAC is always on alongside the economizer.
 * @param maxPower Max power to deliver to the HVAC unit when the economizer is on; 1 is full, 0 is off (not very useful).
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2024
 */
@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record EconomizerSettings(
        double changeoverDelta,
        double targetTemperature,
        Double hvacHandoffFactor,
        Double maxPower
) {

    public EconomizerSettings(double changeoverDelta, double targetTemperature, Double hvacHandoffFactor, Double maxPower) {

        if (changeoverDelta < 0) {
            throw new IllegalArgumentException("changeoverDelta must be non-negative");
        }

        if (maxPower != null && (maxPower.isInfinite() || maxPower.isNaN() || maxPower <= 0 || maxPower > 1)) {
            throw new IllegalArgumentException("maxPower must be in range ]0,1]");
        }

        if (hvacHandoffFactor != null && (hvacHandoffFactor.isInfinite() || hvacHandoffFactor.isNaN() || hvacHandoffFactor < 0)) {
            throw new IllegalArgumentException("hvacHandoffFactor must be non-negative");
        }

        this.changeoverDelta = changeoverDelta;
        this.targetTemperature = targetTemperature;
        this.hvacHandoffFactor = hvacHandoffFactor;
        this.maxPower = maxPower;
    }

    public EconomizerSettings(EconomizerSettings source) {
        this(source.changeoverDelta(), source.targetTemperature(), source.hvacHandoffFactor(), source.maxPower());
    }

    /**
     * Get the effective HVAC handoff factor.
     *
     * @return The configured threshold, or {@link Double#MAX_VALUE} when unconfigured (HVAC fully suppressed).
     */
    public final double getHvacHandoffFactor() {
        return Optional.ofNullable(hvacHandoffFactor).orElse(Double.MAX_VALUE);
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
                && Double.compare(getHvacHandoffFactor(), other.getHvacHandoffFactor()) == 0
                && Double.compare(getMaxPower(), other.getMaxPower()) == 0;
    }
}
