package net.sf.dz3.runtime.config.hardware;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Set;

@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record UnitControllerConfig(
        Set<SingleStageUnitControllerConfig> singleStage,
        Set<MultiStageUnitControllerConfig> multiStage
) {
}
