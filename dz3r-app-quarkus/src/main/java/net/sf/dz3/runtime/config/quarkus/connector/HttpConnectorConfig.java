package net.sf.dz3.runtime.config.quarkus.connector;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public interface HttpConnectorConfig {
    @JsonProperty("id")
    String id();
    @JsonProperty("uri")
    String uri();
    @JsonProperty("zones")
    Set<String> zones();
}
