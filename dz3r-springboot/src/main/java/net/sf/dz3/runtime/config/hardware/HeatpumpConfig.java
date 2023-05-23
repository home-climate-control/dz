package net.sf.dz3.runtime.config.hardware;

public record HeatpumpConfig(
        String id,
        String switchMode,
        String switchRunning,
        String switchFan
) {
}
