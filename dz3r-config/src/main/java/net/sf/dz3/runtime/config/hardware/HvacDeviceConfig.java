package net.sf.dz3.runtime.config.hardware;

import java.util.Set;

public record HvacDeviceConfig(
        Set<SwitchableHvacDeviceConfig> switchable,
        Set<HeatpumpHATConfig> heatpumpHat,
        Set<HeatpumpConfig> heatpump
) {
}
