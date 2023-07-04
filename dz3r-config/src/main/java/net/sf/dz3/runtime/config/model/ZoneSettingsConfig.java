package net.sf.dz3.runtime.config.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record ZoneSettingsConfig(
        Boolean enabled,
        Double setpoint,
        RangeConfig setpointRange,
        Boolean voting,
        boolean hold,
        Integer dumpPriority
) {
}
