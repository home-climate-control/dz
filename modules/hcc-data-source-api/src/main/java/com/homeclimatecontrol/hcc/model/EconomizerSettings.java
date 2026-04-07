package com.homeclimatecontrol.hcc.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Optional;

/**
 * Runtime economizer settings in a form that can be exposed to external systems.
 *
 * @param changeoverDelta Temperature difference between indoor and outdoor temperature necessary to turn the device on.
 * @param targetTemperature When this temperature is reached, the device is shut off.
 * @param hvacHandoffFactor Multiplier (range {@code [0,1]}) applied to the HVAC demand signal when the economizer is active.
 * {@code 0.0} suppresses the HVAC entirely; {@code 1.0} (or {@code null}) passes HVAC demand through unchanged;
 * values between {@code 0} and {@code 1} proportionally reduce the demand handed off to the HVAC unit.
 * The more powerful is your economizer, the lower this value can be. If your economizer injects fresh air into HVAC return,
 * you want to keep this value at {@code 1.0} (which is also a safe default).
 * @param maxPower Max power to deliver to the economizer when it is on (range {@code ]0,1]}); 1 is full, 0 is off (not very useful).
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2026
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

        if (hvacHandoffFactor != null && (hvacHandoffFactor.isInfinite() || hvacHandoffFactor.isNaN() || hvacHandoffFactor < 0 || hvacHandoffFactor > 1)) {
            throw new IllegalArgumentException("hvacHandoffFactor must be in range [0,1]");
        }

        this.changeoverDelta = changeoverDelta;
        this.targetTemperature = targetTemperature;
        this.hvacHandoffFactor = hvacHandoffFactor;
        this.maxPower = maxPower;
    }

    public EconomizerSettings(EconomizerSettings source) {
        this(source.changeoverDelta(), source.targetTemperature(), source.hvacHandoffFactor(), source.maxPower());
    }

    public final double getHvacHandoffFactor() {
        return Optional.ofNullable(hvacHandoffFactor).orElse(1.0);
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
