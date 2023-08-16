package net.sf.dz3.runtime.config.quarkus.hardware;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;

public interface HeatpumpHATConfig {
    @JsonProperty("id")
    String id();
    @JsonProperty("filter")
    Optional<FilterConfig> filter();
}
