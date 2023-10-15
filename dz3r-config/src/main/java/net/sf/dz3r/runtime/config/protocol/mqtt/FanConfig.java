package net.sf.dz3r.runtime.config.protocol.mqtt;

import java.time.Duration;

/**
 * Configuration entry for fans.
 *
 * @param id Identifier, optional (defaults to {@link #address} if absent).
 * @param address Device address. Mandatory.
 * @param availability Availability topic. Mandatory.
 * @param heartbeat Issue identical control commands to this switch at least this often, repeat if necessary.
 * @param pace Issue identical control commands to this switch at most this often.
 */
public record FanConfig(
        String id,
        String address,
        String availability,
        Duration heartbeat,
        Duration pace
) {
}
