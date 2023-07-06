package net.sf.dz3.runtime.config.hardware;

import java.util.List;

public record MultiStageUnitControllerConfig(
        String id,
        List<Double> stages
) {
}
