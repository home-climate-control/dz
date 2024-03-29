package net.sf.dz3r.runtime.config.quarkus.hardware;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;

public interface FilterConfig {
    @JsonProperty("lifetime")
    Duration lifetime();
}
