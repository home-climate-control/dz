package net.sf.dz3.runtime.config.hardware;

import java.time.Duration;

public record HeatpumpHATConfig(
        String id,
        Duration modeChangeDelay,
        FilterConfig filter
) {
}
