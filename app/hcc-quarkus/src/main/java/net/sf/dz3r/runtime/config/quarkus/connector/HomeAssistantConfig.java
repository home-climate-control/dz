package net.sf.dz3r.runtime.config.quarkus.connector;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.sf.dz3r.runtime.config.quarkus.protocol.mqtt.MqttBrokerConfig;

import java.util.Optional;
import java.util.Set;

public interface HomeAssistantConfig {
    @JsonProperty("id")
    String id();
    @JsonProperty("broker")
    MqttBrokerConfig broker();
    @JsonProperty("discovery-prefix")
    Optional<String> discoveryPrefix();
    @JsonProperty("zones")
    Set<String> zones();
}
