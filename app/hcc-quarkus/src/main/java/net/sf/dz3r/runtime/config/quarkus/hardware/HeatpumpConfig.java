package net.sf.dz3r.runtime.config.quarkus.hardware;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;
import java.util.Optional;

public interface HeatpumpConfig {
    @JsonProperty("id")
    String id();
    @JsonProperty("switch-mode")
    String switchMode();
    @JsonProperty("switch-mode-reverse")
    Optional<Boolean> switchModeReverse();
    @JsonProperty("switch-running")
    String switchRunning();
    @JsonProperty("switch-running-reverse")
    Optional<Boolean> switchRunningReverse();
    @JsonProperty("switch-fan")
    String switchFan();
    @JsonProperty("switch-fan-reverse")
    Optional<Boolean> switchFanReverse();
    @JsonProperty("mode-change-delay")
    Optional<Duration> modeChangeDelay();
    @JsonProperty("filter")
    Optional<FilterConfig> filter();
}
