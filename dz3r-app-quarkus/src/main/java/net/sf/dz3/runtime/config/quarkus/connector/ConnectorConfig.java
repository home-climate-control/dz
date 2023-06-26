package net.sf.dz3.runtime.config.quarkus.connector;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;

public interface ConnectorConfig {
    @JsonProperty("http")
    Optional<HttpConnectorConfig> http();
    @JsonProperty("influx")
    Optional<InfluxCollectorConfig> influx();
}
