package net.sf.dz3r.runtime.config.quarkus.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.sf.dz3r.model.HvacMode;

import java.util.Map;
import java.util.Set;

/**
 * Configuration for {@link net.sf.dz3r.model.UnitDirector}.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public interface UnitDirectorConfig {
    @JsonProperty("id")
    String id();
    @JsonProperty("connectors")
    Set<String> connectors();
    @JsonProperty("sensor-feed-mapping")
    Map<String, String> sensorFeedMapping();
    @JsonProperty("unit")
    String unit();
    @JsonProperty("hvac")
    String hvac();
    @JsonProperty("mode")
    HvacMode mode();
}
