package net.sf.dz3r.runtime.config.protocol.mqtt;

import java.util.Optional;

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
        return "mqtt://" + Optional.ofNullable(host()).orElse("localhost")
                + ":" + Optional.ofNullable(port()).orElse(1883)
                + ",autoReconnect=" + autoReconnect();
    }

    String username();

    String password();
}
