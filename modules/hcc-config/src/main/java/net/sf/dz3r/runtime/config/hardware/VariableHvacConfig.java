package net.sf.dz3r.runtime.config.hardware;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record VariableHvacConfig(
        String id,
        String mode,
        String actuator,
        Double maxPower,
        Integer bandCount,
        FilterConfig filter
) implements GenericHvacDeviceConfig {
}
