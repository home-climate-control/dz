package net.sf.dz3r.runtime.config.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Sensor mapping configuration for a zone.
 *
 * More zone sensors can be added later; this is the place they will go to.
 *
 * @param temperature Temperature sensor ID.
 * @param humidity Humidity sensor ID.
 * @param atmosphericPressure Atmospheric pressure sensor ID.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2026
 */
@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record SensorMappingConfig(
        String temperature,
        String humidity,
        String atmosphericPressure
) {
}
