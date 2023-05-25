package net.sf.dz3.runtime.config.model;

import net.sf.dz3r.model.HvacMode;

import java.util.Map;

/**
 * Configuration for {@link net.sf.dz3r.model.UnitDirector}.
 *
 * @param connectors A set of both {@link net.sf.dz3r.view.Connector} and {@link net.sf.dz3r.view.MetricsCollector} IDs.
 * @param sensorFeedMapping
 */
public record UnitDirectorConfig(
        String id,
        String connectors,
        Map<String, String> sensorFeedMapping,
        String unit,
        String hvac,
        HvacMode mode
) {
}
