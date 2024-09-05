package net.sf.dz3r.runtime.config.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.homeclimatecontrol.hcc.model.HvacMode;
import net.sf.dz3r.runtime.config.Identifiable;

import java.util.Map;
import java.util.Set;

/**
 * Configuration for {@link net.sf.dz3r.model.UnitDirector}.
 *
 * @param connectors A set of both {@link net.sf.dz3r.view.Connector} and {@link net.sf.dz3r.view.MetricsCollector} IDs.
 * @param sensorFeedMapping Mapping from the sensor to the zone it serves. TODO: Make this explicit with a Java record, the order is confusing and it easy to mix the key and the value.
 */
@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record UnitDirectorConfig(
        String id,
        Set<String> connectors,
        Map<String, String> sensorFeedMapping,
        String unit,
        String hvac,
        HvacMode mode
) implements Identifiable {
}
