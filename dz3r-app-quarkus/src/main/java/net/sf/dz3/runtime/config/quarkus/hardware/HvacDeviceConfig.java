package net.sf.dz3.runtime.config.quarkus.hardware;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public interface HvacDeviceConfig {
    @JsonProperty("switchable")
    Set<SwitchableHvacDeviceConfig> switchable();
    @JsonProperty("heatpump-hat")
    Set<HeatpumpHATConfig> heatpumpHat();
    @JsonProperty("heatpump")
    Set<HeatpumpConfig> heatpump();
}
