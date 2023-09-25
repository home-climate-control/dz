package net.sf.dz3r.runtime.config.connector;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

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
) {
}
