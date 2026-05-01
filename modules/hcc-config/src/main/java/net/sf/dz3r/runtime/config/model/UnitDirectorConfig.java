package net.sf.dz3r.runtime.config.model;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import com.homeclimatecontrol.hcc.model.HvacMode;
import net.sf.dz3r.runtime.config.Identifiable;

import java.util.Map;
import java.util.Set;

/**
 * Configuration for {@link net.sf.dz3r.model.UnitDirector}.
 *
 * @param connectors A set of both {@link net.sf.dz3r.view.Connector} and {@link net.sf.dz3r.view.MetricsCollector} IDs.
 * @param sensorFeedMapping Mapping from the sensor to the zone it serves.
 * Deprecated (the order is confusing and it's easy to mix the key and the value), use {@link #sensorMapping} instead.
 * @param sensorMapping Mapping from the zone name to the set of sensors it feeds from.
 */
@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record UnitDirectorConfig(
        String id,
        Set<String> connectors,
        Map<String, String> sensorFeedMapping,
        Map<String, SensorMappingConfig> sensorMapping,
        String unit,
        String hvac,
        HvacMode mode
) implements Identifiable {
}
