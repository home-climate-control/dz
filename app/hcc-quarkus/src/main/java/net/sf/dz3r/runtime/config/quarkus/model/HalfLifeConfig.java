package net.sf.dz3r.runtime.config.quarkus.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.Duration;
import java.util.Optional;

/**
 * {@link net.sf.dz3r.controller.HalfLifeController} configuration.
 *
 * @param halfLife See <a href="https://www.omnicalculator.com/chemistry/half-life#half-life-formula">half life formulas</a> for explanation.
 * @param multiplier Multiply the standard {@link net.sf.dz3r.controller.HalfLifeController} output signal by this to obtain the effective output value. Must be positive.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2024
 */
@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record HalfLifeConfig(
        @JsonProperty("half-life")
        Optional<Duration> halfLife,
        @JsonProperty("multiplier")
        Optional<Double> multiplier
) {
}
