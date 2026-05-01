package net.sf.dz3r.runtime.config.hardware;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.Set;

@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record UnitControllerConfig(
        Set<SingleStageUnitControllerConfig> singleStage,
        Set<MultiStageUnitControllerConfig> multiStage
) {
}
