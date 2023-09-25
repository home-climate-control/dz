package net.sf.dz3r.runtime.config.quarkus.hardware;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;
import java.util.Optional;

/**
 * Configuration entry for switches.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public interface SwitchConfig {
    @JsonProperty("id")
    Optional<String> id();
    @JsonProperty("address")
    String address();
    @JsonProperty("reversed")
    Optional<Boolean> reversed();
    @JsonProperty("heartbeat")
    Optional<Duration> heartbeat();
    @JsonProperty("pace")
    Optional<Duration> pace();
    @JsonProperty("optimistic")
    Optional<Boolean> optimistic();
}
