package net.sf.dz3.runtime.config.quarkus.protocol.mqtt;

import java.util.Optional;

/**
 * Defines a unique MQTT broker connection.
 */
public interface MqttEndpointSpec {
    String host();
    Optional<Integer> port();

    Optional<Boolean> autoReconnect();

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
                + ":" + Optional.ofNullable(port()).orElse(Optional.of(1883))
                + ",autoReconnect=" + autoReconnect();
    }

    Optional<String> username();

    Optional<String> password();
}
