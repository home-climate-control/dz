package net.sf.dz3r.runtime.config.quarkus.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.sf.dz3r.model.Zone;

import java.util.Optional;

/**
 * {@link Zone configuration in Quarkus notation (as an interface).
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2024
 */
public interface ZoneConfig {
    @JsonProperty("id")
    String id();
    @JsonProperty("name")
    String name();
    @JsonProperty("controller")
    PidControllerConfig controller();
    @JsonProperty("sensitivity")
    Optional<HalfLifeConfig> sensitivity();
    @JsonProperty("settings")
    ZoneSettingsConfig settings();
    @JsonProperty("economizer")
    Optional<EconomizerConfig> economizer();
}
