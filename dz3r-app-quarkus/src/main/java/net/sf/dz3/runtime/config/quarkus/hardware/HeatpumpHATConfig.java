package net.sf.dz3.runtime.config.quarkus.hardware;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface HeatpumpHATConfig {
    @JsonProperty("id")
    String id();
}
