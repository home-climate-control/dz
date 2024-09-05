package net.sf.dz3r.runtime.config.quarkus.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.homeclimatecontrol.hcc.model.HvacMode;

import java.time.Duration;
import java.util.Optional;

public interface EconomizerConfig {
    @JsonProperty("ambient-sensor")
    String ambientSensor();
    @JsonProperty("controller")
    PidControllerConfig controller();
    @JsonProperty("mode")
    HvacMode mode();
    @JsonProperty("hvac-device")
    String hvacDevice();
    @JsonProperty("timeout")
    Optional<Duration> timeout();
    @JsonProperty("settings")
    Optional<Settings> settings();

    interface Settings {
        @JsonProperty("changeover-delta")
        double changeoverDelta();
        @JsonProperty("target-temperature")
        double targetTemperature();
        @JsonProperty("keep-hvac-on")
        Boolean keepHvacOn();
    }
}
