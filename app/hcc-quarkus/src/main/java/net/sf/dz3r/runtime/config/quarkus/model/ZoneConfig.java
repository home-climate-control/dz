package net.sf.dz3r.runtime.config.quarkus.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;

public interface ZoneConfig {
    @JsonProperty("id")
    String id();
    @JsonProperty("name")
    String name();
    @JsonProperty("controller")
    PidControllerConfig controller();
    @JsonProperty("settings")
    ZoneSettingsConfig settings();
    @JsonProperty("economizer")
    Optional<EconomizerConfig> economizer();
}
