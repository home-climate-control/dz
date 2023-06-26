package net.sf.dz3.runtime.config.quarkus.protocol.mqtt;

import java.util.Optional;

/**
 * Full set of MQTT broker constructor arguments.
 */
public interface MqttBrokerSpec extends MqttEndpointSpec {
    String rootTopic();

    @Override
    default String signature() {
        return "mqtt://" + Optional.ofNullable(host()).orElse("localhost")
                + ":" + Optional.ofNullable(port()).orElse(Optional.of(1883))
                + ",autoReconnect=" + autoReconnect()
                + ",rootTopic=" + rootTopic();
    }
}
