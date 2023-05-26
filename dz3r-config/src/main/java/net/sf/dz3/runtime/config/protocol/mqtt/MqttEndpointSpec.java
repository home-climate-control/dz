package net.sf.dz3.runtime.config.protocol.mqtt;

/**
 * Defines a unique MQTT broker connection.
 */
public interface MqttEndpointSpec {
    String host();
    Integer port();

    /**
     * @deprecated Move to {@link MqttBrokerSpec} as soon as {@code ESPHomeListener} is refactored to open one connection to one endpoint.
     */
    @Deprecated(forRemoval = false)
    String rootTopic();

    boolean autoReconnect();
}
