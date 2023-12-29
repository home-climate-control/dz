package net.sf.dz3r.runtime.config.quarkus.protocol.mqtt;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;

public interface MqttBrokerConfig extends MqttBrokerSpec {
    @JsonProperty
    Optional<String> id();

    @JsonProperty("host")
    @Override
    String host();

    @JsonProperty("port")
    @Override
    Optional<Integer> port();

    @JsonProperty("username")
    @Override
    Optional<String> username();

    @JsonProperty("password")
    @Override
    Optional<String> password();
    @JsonProperty("root-topic")
    @Override
    Optional<String> rootTopic();

    @JsonProperty("auto-reconnect")
    @Override
    Optional<Boolean> autoReconnect();
}
