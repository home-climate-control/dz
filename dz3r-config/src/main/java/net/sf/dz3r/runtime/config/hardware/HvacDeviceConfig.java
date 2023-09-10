package net.sf.dz3r.runtime.config.hardware;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Set;

@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record HvacDeviceConfig(
        Set<SwitchableHvacDeviceConfig> switchable,
        Set<HeatpumpHATConfig> heatpumpHat,
        Set<HeatpumpConfig> heatpump
) {
}
