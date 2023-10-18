package net.sf.dz3r.runtime.config.hardware;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record VariableHvacConfig(
        String id,
        String mode,
        String actuator,
        Integer bandCount,
        FilterConfig filter
) {
}
