package net.sf.dz3r.runtime.config.quarkus.hardware;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;
import java.util.Optional;

public interface HeatpumpHATConfig {
    @JsonProperty("id")
    String id();
    @JsonProperty("mode-change-delay")
    Optional<Duration> modeChangeDelay();
    @JsonProperty("filter")
    Optional<FilterConfig> filter();
}
