package net.sf.dz3r.runtime.config.hardware;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import net.sf.dz3r.runtime.config.Identifiable;

import java.time.Duration;

/**
 * Configuration entry for switches.
 *
 * @param id Identifier, optional (defaults to {@link #address} if absent).
 * @param address Device address. Mandatory.
 * @param reversed {@code true} if the switch must be reversed.
 * @param heartbeat Issue identical control commands to this switch at least this often, repeat if necessary.
 * @param pace Issue identical control commands to this switch at most this often.
 * @param availabilityTopic Topic where MQTT switch availability information is published. Will cause {@link IllegalArgumentException} for other types.
 */
@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record SwitchConfig(
        String id,
        String address,
        boolean reversed,
        Duration heartbeat,
        Duration pace,
        String availabilityTopic
) implements Identifiable {
}
