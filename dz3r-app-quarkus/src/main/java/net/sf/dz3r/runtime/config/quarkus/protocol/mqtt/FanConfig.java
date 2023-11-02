package net.sf.dz3r.runtime.config.quarkus.protocol.mqtt;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;
import java.util.Optional;

public interface FanConfig {
    @JsonProperty("id")
    Optional<String> id();
    @JsonProperty("address")
    String address();
    @JsonProperty("heartbeat")
    Optional<Duration> heartbeat();
    @JsonProperty("pace")
    Optional<Duration> pace();
    @JsonProperty("availability-topic")
    Optional<String> availabilityTopic();
}
