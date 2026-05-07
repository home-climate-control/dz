package net.sf.dz3r.runtime.config.hardware;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.Set;

@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record HvacDeviceConfig(
        Set<SwitchableHvacDeviceConfig> switchable,
        Set<HeatpumpHATConfig> heatpumpHat,
        Set<HeatpumpConfig> heatpump,
        Set<VariableHvacConfig> variable
) {
}
