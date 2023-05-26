package net.sf.dz3.runtime.config.protocol.mqtt;

/**
 * Defines a unique MQTT broker connection.
 */
public interface MqttEndpointSpec {
    String host();
    Integer port();

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
        return "mqtt://" + (host() == null ? "localhost" : host())
                + ":" + (port() == null ? "1883" : port())
                + ",autoReconnect=" + autoReconnect();
    }

    String username();

    String password();
}
