package net.sf.dz3.runtime.config.quarkus.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;

public interface ZoneSettingsConfig {
    @JsonProperty("enabled")
    Optional<Boolean> enabled();
    @JsonProperty("setpoint")
    Double setpoint();
    @JsonProperty("setpoint-range")
    Optional<RangeConfig> setpointRange();
    @JsonProperty("voting")
    Optional<Boolean> voting();
    @JsonProperty("hold")
    Optional<Boolean> hold();
    @JsonProperty("dumpPriority")
    Optional<Integer> dumpPriority();
}
