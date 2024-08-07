package net.sf.dz3r.runtime.config.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.Duration;

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
        Duration halfLife,
        Double multiplier
) {
}
