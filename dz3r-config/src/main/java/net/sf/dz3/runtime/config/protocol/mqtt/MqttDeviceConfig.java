package net.sf.dz3.runtime.config.protocol.mqtt;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import net.sf.dz3.runtime.config.hardware.SensorConfig;
import net.sf.dz3.runtime.config.hardware.SwitchConfig;

import java.util.Set;

/**
 * Configuration entry for MQTT devices.
 *
 * @param id Identifier, optional (defaults to {@link #host} if absent).
 * @param host MQTT broker host.
 * @param port MQTT broker port. Defaults to 1883 if absent.
 * @param rootTopic MQTT root topic. Mandatory.
 * @param sensors Set of sensors, optional.
 * @param switches Set of switches, optional.
 */
@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record MqttDeviceConfig(
        String id,
        String host,
        Integer port,
        String username,
        String password,
        String rootTopic,
        boolean autoReconnect,
        Set<SensorConfig> sensors,
        Set<SwitchConfig> switches
) implements MqttGateway {
}
