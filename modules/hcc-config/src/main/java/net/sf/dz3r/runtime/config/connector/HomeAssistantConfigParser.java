package net.sf.dz3r.runtime.config.connector;

import net.sf.dz3r.runtime.config.protocol.mqtt.MqttBrokerConfig;

import java.util.Optional;

public interface HomeAssistantConfigParser {

    MqttBrokerConfig broker();
    String discoveryPrefix();

    default MqttBrokerConfig parse() {

        // Some sanity checking first

        // VT: NOTE: With existing interfaces and records, either Quarkus will choke on missing root topic,
        // or I will spend inordinate time refactoring it. So, Worse is Better.
        // Just require one of (broker.root-topic, discovery-prefix) to be present.

        if (broker().rootTopic() != null && discoveryPrefix() != null) {
            throw new IllegalArgumentException("both broker.root-topic and discovery-prefix are present, must specify only one");
        }

        var topic = Optional.ofNullable(broker().rootTopic())
                .orElse(Optional.ofNullable(discoveryPrefix())
                        .orElse("homeassistant"));
        var id = Optional.ofNullable(broker().id()).orElse(Integer.toHexString((broker().host() + ":" + broker().port()).hashCode()));

        return new MqttBrokerConfig(
                id,
                broker().host(),
                broker().port(),
                broker().username(),
                broker().password(),
                topic,
                broker().autoReconnect()
        );
    }
}
