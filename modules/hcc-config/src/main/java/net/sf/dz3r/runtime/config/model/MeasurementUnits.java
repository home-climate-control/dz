package net.sf.dz3r.runtime.config.model;

/**
 * System wide measurement units configuration.
 *
 * @param temperature Temperature unit. Defaults to degrees Celsius.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2024
 */
public record MeasurementUnits(
        TemperatureUnit temperature
) {
}
