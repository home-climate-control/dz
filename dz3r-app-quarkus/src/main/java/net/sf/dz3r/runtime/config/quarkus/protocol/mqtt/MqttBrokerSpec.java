package net.sf.dz3r.runtime.config.quarkus.protocol.mqtt;

import java.util.Optional;

/**
 * Full set of MQTT broker constructor arguments.
 */
public interface MqttBrokerSpec extends MqttEndpointSpec {

    /**
     * Root topic.
     *
     * While mandatory for the implementation, it may be implied by enclosing elements, hence {@code Optional}.
     * It is a caller responsibility to verify that this value is present if needed.
     */
    Optional<String> rootTopic();

    @Override
    default String signature() {
        return "mqtt://" + Optional.ofNullable(host()).orElse("localhost")
                + ":" + Optional.ofNullable(port()).orElse(Optional.of(1883))
                + ",autoReconnect=" + autoReconnect()
                + ",rootTopic=" + rootTopic();
    }
}
