package net.sf.dz3r.runtime.config.quarkus.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.sf.dz3r.model.HvacMode;

public interface EconomizerConfig {
    @JsonProperty("ambient-sensor")
    String ambientSensor();
    @JsonProperty("changeover-delta")
    double changeoverDelta();
    @JsonProperty("target-temperature")
    double targetTemperature();
    @JsonProperty("keep-hvac-on")
    Boolean keepHvacOn();
    @JsonProperty("controller")
    PidControllerConfig controller();
    @JsonProperty("mode")
    HvacMode mode();
    @JsonProperty("hvac-device")
    String hvacDevice();
}
