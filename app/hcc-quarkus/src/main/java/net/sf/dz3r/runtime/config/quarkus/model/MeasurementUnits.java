package net.sf.dz3r.runtime.config.quarkus.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.sf.dz3r.runtime.config.model.TemperatureUnit;

import java.util.Optional;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
public interface MeasurementUnits {
    @JsonPropertyOrder("temperature")
    Optional<TemperatureUnit> temperature();
}
