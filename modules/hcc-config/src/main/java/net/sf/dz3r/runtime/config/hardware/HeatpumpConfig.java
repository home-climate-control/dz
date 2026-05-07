package net.sf.dz3r.runtime.config.hardware;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.Duration;

@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record HeatpumpConfig(
        String id,
        String switchMode,
        Boolean switchModeReverse,
        String switchRunning,
        Boolean switchRunningReverse,
        String switchFan,
        Boolean switchFanReverse,
        Duration modeChangeDelay,
        FilterConfig filter
) implements GenericHvacDeviceConfig {
}
