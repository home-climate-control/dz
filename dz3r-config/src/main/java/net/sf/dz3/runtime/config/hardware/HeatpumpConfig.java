package net.sf.dz3.runtime.config.hardware;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

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
) {
}
