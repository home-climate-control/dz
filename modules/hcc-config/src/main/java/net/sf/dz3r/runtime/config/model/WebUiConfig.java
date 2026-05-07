package net.sf.dz3r.runtime.config.model;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.Set;

@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record WebUiConfig(
        Integer httpPort,
        Integer duplexPort,
        String interfaces,
        TemperatureUnit units,
        Set<String> directors) {
}
