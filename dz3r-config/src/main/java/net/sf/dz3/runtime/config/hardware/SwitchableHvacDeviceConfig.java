package net.sf.dz3.runtime.config.hardware;

public record SwitchableHvacDeviceConfig(
        String id,
        String mode,
        String switchAddress
) {
}
