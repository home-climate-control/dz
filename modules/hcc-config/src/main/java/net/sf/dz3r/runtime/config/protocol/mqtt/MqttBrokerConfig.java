package net.sf.dz3r.runtime.config.protocol.mqtt;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import net.sf.dz3r.runtime.config.Identifiable;

/**
 * MQTT Broker endpoint configuration.
 *
 * @param id Identifier, optional (defaults to {@link #host} if absent). Used to identify this client to the MQTT server.
 * @param host MQTT broker host.
 * @param port MQTT broker port. Defaults to 1883 if absent.
 * @param rootTopic MQTT root topic. Mandatory.
 */
@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record MqttBrokerConfig(
        String id,
        String host,
        Integer port,
        String username,
        String password,
        String rootTopic,
        boolean autoReconnect
) implements MqttBrokerSpec, Identifiable {
}
