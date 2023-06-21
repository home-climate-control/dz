package net.sf.dz3.runtime.config.hardware;

import java.util.Set;

public record UnitControllerConfig(
        Set<SingleStageUnitControllerConfig> singleStage,
        Set<MultiStageUnitControllerConfig> multiStage
) {
}
