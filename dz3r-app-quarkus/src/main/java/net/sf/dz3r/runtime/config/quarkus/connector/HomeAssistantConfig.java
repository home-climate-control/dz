package net.sf.dz3r.runtime.config.quarkus.connector;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.sf.dz3r.runtime.config.quarkus.protocol.mqtt.MqttBrokerConfig;

import java.util.Optional;

public interface HomeAssistantConfig {
    @JsonProperty("broker")
    MqttBrokerConfig broker();
    @JsonProperty("discovery-prefix")
    Optional<String> discoveryPrefix();
    @JsonProperty("node-id")
    Optional<String> nodeId();
}
