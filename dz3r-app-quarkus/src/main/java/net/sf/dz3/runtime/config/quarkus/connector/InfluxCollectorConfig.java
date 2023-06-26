package net.sf.dz3.runtime.config.quarkus.connector;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Optional;

public interface InfluxCollectorConfig {
    @JsonProperty("id")
    String id();
    @JsonProperty("instance")
    String instance();
    @JsonProperty("db")
    String db();
    @JsonProperty("uri")
    String uri();
    @JsonProperty("username")
    Optional<String> username();
    @JsonProperty("password")
    Optional<String> password();
    @JsonProperty("sensor-feed-mapping")
    Map<String, String> sensorFeedMapping();
}
