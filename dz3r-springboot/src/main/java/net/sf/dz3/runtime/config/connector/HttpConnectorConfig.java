package net.sf.dz3.runtime.config.connector;

import java.util.Set;

public record HttpConnectorConfig(
        String id,
        String uri,
        Set<String> zones
) {
}
