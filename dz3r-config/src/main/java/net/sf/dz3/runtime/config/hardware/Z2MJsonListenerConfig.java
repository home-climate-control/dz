package net.sf.dz3.runtime.config.hardware;

import java.util.List;

/**
 * Configuration entry for {@link net.sf.dz3r.device.z2m.v1.Z2MJsonListener}.
 *
 * @param id Identifier, optional (defaults to {@link #host} if absent).
 * @param host MQTT broker host.
 * @param port MQTT broker port. Defaults to 1883 if absent.
 * @param rootTopic MQTT root topic. Mandatory.
 * @param reconnect Attempt to automatically reconnect if {@code true}.
 * @param sensors List of sensors, optional.
 * @param switches List of switches, optional.
 */
public record Z2MJsonListenerConfig(
    String id,
    String host,
    Integer port,
    String username,
    String password,
    String rootTopic,
    boolean reconnect,
    List<SensorConfig> sensors,
    List<SwitchConfig> switches) {
}
