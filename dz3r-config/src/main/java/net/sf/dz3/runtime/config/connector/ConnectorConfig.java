package net.sf.dz3.runtime.config.connector;

public record ConnectorConfig(
        HttpConnectorConfig http,
        InfluxCollectorConfig influx
) {
}
