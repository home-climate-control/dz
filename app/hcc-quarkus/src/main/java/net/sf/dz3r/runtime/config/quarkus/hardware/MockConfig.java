package net.sf.dz3r.runtime.config.quarkus.hardware;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public interface MockConfig {
    @JsonProperty("switches")
    Set<SwitchConfig> switches();
}
