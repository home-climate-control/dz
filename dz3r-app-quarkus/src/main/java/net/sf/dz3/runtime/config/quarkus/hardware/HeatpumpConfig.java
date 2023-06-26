package net.sf.dz3.runtime.config.quarkus.hardware;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface HeatpumpConfig {
    @JsonProperty("id")
    String id();
    @JsonProperty("switch-mode")
    String switchMode();
    @JsonProperty("switch-running")
    String switchRunning();
    @JsonProperty("switch-fan")
    String switchFan();
}
