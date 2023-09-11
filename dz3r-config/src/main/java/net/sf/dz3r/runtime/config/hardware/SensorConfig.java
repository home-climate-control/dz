package net.sf.dz3r.runtime.config.hardware;

import java.time.Duration;

/**
 * Configuration entry for sensors.
 *
 * @param id Identifier, optional (defaults to {@link #address} if absent).
 * @param address Device address. Mandatory.
 * @param measurement Expected measurement. If present, will be checked against the device type, otherwise ignored.
 * @param step Expected interval between measurements. May be unavailable for some implementations.
 */
public record SensorConfig(
        String id,
        String address,
        String measurement,
        Duration step
) {
}
