package net.sf.dz3r.runtime.config.quarkus.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.homeclimatecontrol.hcc.model.HvacMode;
import net.sf.dz3r.runtime.config.model.SensorMappingConfig;

import java.util.Map;
import java.util.Set;

/**
 * Configuration for {@link net.sf.dz3r.model.UnitDirector}.
 *
 * @see net.sf.dz3r.runtime.config.model.UnitDirectorConfig
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2026
 */
public interface UnitDirectorConfig {
    @JsonProperty("id")
    String id();
    @JsonProperty("connectors")
    Set<String> connectors();
    @JsonProperty("sensor-feed-mapping")
    Map<String, String> sensorFeedMapping();
    @JsonProperty("sensor-mapping")
    Map<String, SensorMappingConfig> sensorMapping();
    @JsonProperty("unit")
    String unit();
    @JsonProperty("hvac")
    String hvac();
    @JsonProperty("mode")
    HvacMode mode();
}
