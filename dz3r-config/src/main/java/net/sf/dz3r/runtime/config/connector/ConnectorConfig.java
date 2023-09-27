package net.sf.dz3r.runtime.config.connector;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record ConnectorConfig(
        HttpConnectorConfig http,
        InfluxCollectorConfig influx,
        HomeAssistantConfig homeAssistant
) {
}
