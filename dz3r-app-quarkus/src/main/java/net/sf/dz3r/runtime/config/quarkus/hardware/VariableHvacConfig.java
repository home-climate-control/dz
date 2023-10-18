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
    @JsonProperty("filter")
    Optional<FilterConfig> filter();
}
