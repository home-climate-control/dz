package net.sf.dz3r.runtime.config.protocol.mqtt;

import java.util.Optional;

/**
 * Full set of MQTT broker constructor arguments.
 */
public interface MqttBrokerSpec extends MqttEndpointSpec {
    String rootTopic();

    @Override
    default String signature() {
        return "mqtt://" + Optional.ofNullable(host()).orElse("localhost")
                + ":" + Optional.ofNullable(port()).orElse(1883)
                + ",autoReconnect=" + autoReconnect()
                + ",rootTopic=" + rootTopic();
    }
}
