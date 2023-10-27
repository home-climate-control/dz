package net.sf.dz3r.runtime.config.protocol.mqtt;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.Duration;

/**
 * Configuration entry for fans.
 *
 * @param id Identifier, optional (defaults to {@link #address} if absent).
 * @param address Device address. Mandatory.
 * @param heartbeat Issue identical control commands to this switch at least this often, repeat if necessary.
 * @param pace Issue identical control commands to this switch at most this often.
 * @param availabilityTopic Availability topic. Mandatory.
 */
@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record FanConfig(
        String id,
        String address,
        Duration heartbeat,
        Duration pace,
        String availabilityTopic
) {
}
