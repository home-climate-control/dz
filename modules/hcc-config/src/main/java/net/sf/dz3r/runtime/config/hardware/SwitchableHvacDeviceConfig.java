package net.sf.dz3r.runtime.config.hardware;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record SwitchableHvacDeviceConfig(
        String id,
        String mode,
        String switchAddress,
        Boolean switchReverse,
        FilterConfig filter
) implements GenericHvacDeviceConfig {
}
