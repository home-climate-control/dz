package net.sf.dz3.runtime.config.hardware;

import java.util.List;

public record HvacDeviceConfig(
        List<SwitchableHvacDeviceConfig> switchable,
        List<HeatpumpHATConfig> heatpumpHat,
        List<HeatpumpConfig> heatpump
) {
}
