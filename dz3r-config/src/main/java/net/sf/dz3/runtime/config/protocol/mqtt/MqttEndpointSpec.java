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

    /**
     * Get a unique signature.
     *
     * Credentials are not included - their mutual exclusivity is guaranteed by the broker configuration
     * (good luck having two brokers, one anonymous and the other not, on the same host and port).
     *
     * @return A unique signature of (host, port, autoReconnect).
     */
    default String signature() {
        return "host=" + (host() == null ? "localhost" : host())
                + ",port=" + (port() == null ? "1883" : port())
                + ",autoReconnect=" + autoReconnect();
    }

    String username();

    String password();
}
