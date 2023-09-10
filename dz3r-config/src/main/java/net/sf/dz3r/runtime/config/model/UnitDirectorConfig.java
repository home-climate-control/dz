package net.sf.dz3r.runtime.config.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import net.sf.dz3r.model.HvacMode;

import java.util.Map;
import java.util.Set;

/**
 * Configuration for {@link net.sf.dz3r.model.UnitDirector}.
 *
 * @param connectors A set of both {@link net.sf.dz3r.view.Connector} and {@link net.sf.dz3r.view.MetricsCollector} IDs.
 * @param sensorFeedMapping
 */
@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record UnitDirectorConfig(
        String id,
        Set<String> connectors,
        Map<String, String> sensorFeedMapping,
        String unit,
        String hvac,
        HvacMode mode
) {
}
