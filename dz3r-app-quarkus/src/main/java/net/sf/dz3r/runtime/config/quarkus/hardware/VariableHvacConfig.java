package net.sf.dz3r.runtime.config.quarkus.hardware;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;

public interface VariableHvacConfig {
    @JsonProperty("id")
    String id();
    @JsonProperty("mode")
    String mode();
    @JsonProperty("actuator")
    String actuator();
    @JsonProperty("max-power")
    Optional<Double> maxPower();
    @JsonProperty("band-count")
    Optional<Integer> bandCount();
    @JsonProperty("filter")
    Optional<FilterConfig> filter();
}
