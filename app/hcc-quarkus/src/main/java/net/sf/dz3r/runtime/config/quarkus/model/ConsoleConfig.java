package net.sf.dz3r.runtime.config.quarkus.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.sf.dz3r.runtime.config.model.TemperatureUnit;

import java.util.Optional;
import java.util.Set;

public interface ConsoleConfig {

    @JsonProperty("units")
    Optional<TemperatureUnit> units();

    @JsonProperty("directors")
    Optional<Set<String>> directors();

    @JsonProperty("sensors")
    Optional<Set<String>> sensors();
}
