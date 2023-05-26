package net.sf.dz3.runtime.config.protocol.mqtt;

import net.sf.dz3.runtime.config.hardware.SensorConfig;
import net.sf.dz3.runtime.config.hardware.SwitchConfig;

import java.util.List;

/**
 * Configuration entry for {@link net.sf.dz3r.device.esphome.v1.ESPHomeListener}.
 *
 * @param id Identifier, optional (defaults to {@link #host} if absent).
 * @param host MQTT broker host.
 * @param port MQTT broker port. Defaults to 1883 if absent.
 * @param rootTopic MQTT root topic. Mandatory.
 * @param sensors List of sensors, optional.
 * @param switches List of switches, optional.
 */
public record ESPHomeListenerConfig(
        String id,
        String host,
        Integer port,
        String username,
        String password,
        String rootTopic,
        boolean autoReconnect,
        List<SensorConfig> sensors,
        List<SwitchConfig> switches
) implements MqttGateway {
}
