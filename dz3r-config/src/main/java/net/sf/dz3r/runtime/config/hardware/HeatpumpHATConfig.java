package net.sf.dz3r.runtime.config.hardware;

import java.time.Duration;

public record HeatpumpHATConfig(
        String id,
        Duration modeChangeDelay,
        FilterConfig filter
) {
}
