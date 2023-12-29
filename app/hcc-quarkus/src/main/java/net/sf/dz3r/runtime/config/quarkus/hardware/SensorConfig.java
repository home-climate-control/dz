package net.sf.dz3r.runtime.config.quarkus.hardware;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;
import java.util.Optional;

/**
 * Configuration entry for sensors.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public interface SensorConfig {
    @JsonProperty("id")
    Optional<String> id();
    @JsonProperty("address")
    String address();
    @JsonProperty("measurement")
    Optional<String> measurement();
    @JsonProperty("step")
    Optional<Duration> step();
    @JsonProperty("timeout")
    Optional<Duration> timeout();
}
