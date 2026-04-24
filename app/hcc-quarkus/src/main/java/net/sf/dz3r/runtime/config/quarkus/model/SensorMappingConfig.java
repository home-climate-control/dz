package net.sf.dz3r.runtime.config.quarkus.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @see net.sf.dz3r.runtime.config.model.SensorMappingConfig
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2026
 */
public interface SensorMappingConfig {
    @JsonProperty("temperature")
    String temperature();
    @JsonProperty("humidity")
    String humidity();
    @JsonProperty("atmospheric-pressure")
    String atmosphericPressure();
}
