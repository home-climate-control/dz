package net.sf.dz3r.runtime.config.connector;

public record ConnectorConfig(
        HttpConnectorConfig http,
        InfluxCollectorConfig influx) {
}
