package net.sf.dz3.runtime.config.hardware;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record SwitchableHvacDeviceConfig(
        String id,
        String mode,
        String switchAddress,
        Boolean switchReverse
) {
}
