package net.sf.dz3.runtime.config.quarkus.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;

public interface ZoneConfig {
    @JsonProperty("zone-id")
    String zoneId();
    @JsonProperty("name")
    String name();
    @JsonProperty("controller")
    PidControllerConfig controller();
    @JsonProperty("settings")
    ZoneSettingsConfig settings();
    @JsonProperty("economizer")
    Optional<EconomizerConfig> economizer();
}
