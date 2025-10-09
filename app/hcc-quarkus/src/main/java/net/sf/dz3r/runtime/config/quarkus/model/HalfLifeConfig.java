package net.sf.dz3r.runtime.config.quarkus.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;
import java.util.Optional;

/**
 * {@link net.sf.dz3r.controller.HalfLifeController} configuration.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2025
 */
public interface HalfLifeConfig {

    /**
     * See <a href="https://www.omnicalculator.com/chemistry/half-life#half-life-formula">half life formulas</a> for explanation.
     */
    @JsonProperty("half-life")
    Optional<Duration> halfLife();

    /**
     * Multiply the standard {@link net.sf.dz3r.controller.HalfLifeController} output signal by this to obtain the effective output value. Must be positive.
     */
    @JsonProperty("multiplier")
    Optional<Double> multiplier();
}
