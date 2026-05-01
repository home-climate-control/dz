package net.sf.dz3r.runtime.config.connector;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import net.sf.dz3r.runtime.config.Identifiable;

import java.util.Map;

@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record InfluxCollectorConfig(
        String id,
        String instance,
        String db,
        String uri,
        String username,
        String password,
        Map<String, String> sensorFeedMapping
) implements Identifiable {
}
