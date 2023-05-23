package net.sf.dz3.runtime.config.connector;

import java.util.Map;

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
