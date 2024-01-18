package net.sf.dz3r.runtime.config.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Set;

@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record WebUiConfig(
        Integer httpPort,
        Integer duplexPort,
        String interfaces,
        TemperatureUnit units,
        Set<String> directors) {
}
