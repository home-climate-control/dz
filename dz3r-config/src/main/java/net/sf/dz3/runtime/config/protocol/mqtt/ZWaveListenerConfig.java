package net.sf.dz3.runtime.config.protocol.mqtt;

import net.sf.dz3.runtime.config.hardware.SensorConfig;
import net.sf.dz3.runtime.config.hardware.SwitchConfig;

import java.util.List;

/**
 * Configuration entry for Z=-Wave devices.
 *
 * @param id Identifier, optional (defaults to {@link #host} if absent).
 * @param host MQTT broker host.
 * @param port MQTT broker port. Defaults to 1883 if absent.
 * @param rootTopic MQTT root topic. Mandatory.
 * @param reconnect Attempt to automatically reconnect if {@code true}.
 * @param sensors List of sensors, optional.
 * @param switches List of switches, optional.
 */
public record ZWaveListenerConfig(
    String id,
    String host,
    Integer port,
    String username,
    String password,
    String rootTopic,
    boolean reconnect,
    List<SensorConfig> sensors,
    List<SwitchConfig> switches) implements MqttGateway {
}
