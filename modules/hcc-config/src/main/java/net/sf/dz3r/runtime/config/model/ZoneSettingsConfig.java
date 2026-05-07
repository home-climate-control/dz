package net.sf.dz3r.runtime.config.model;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record ZoneSettingsConfig(
        Boolean enabled,
        Double setpoint,
        RangeConfig setpointRange,
        Boolean voting,
        Boolean hold,
        Integer dumpPriority
) {
}
