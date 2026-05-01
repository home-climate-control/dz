package net.sf.dz3r.runtime.config.connector;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record ConnectorConfig(
        HttpConnectorConfig http,
        InfluxCollectorConfig influx,
        HomeAssistantConfig homeAssistant
) {
}
