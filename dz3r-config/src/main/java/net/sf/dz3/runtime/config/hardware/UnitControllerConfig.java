package net.sf.dz3.runtime.config.hardware;

import java.util.List;

public record UnitControllerConfig(
        List<SingleStageUnitControllerConfig> singleStage,
        List<MultiStageUnitControllerConfig> multiStage
) {
}
