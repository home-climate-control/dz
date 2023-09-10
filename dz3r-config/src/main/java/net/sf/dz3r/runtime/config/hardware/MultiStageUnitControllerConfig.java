package net.sf.dz3r.runtime.config.hardware;

import java.util.List;

public record MultiStageUnitControllerConfig(
        String id,
        List<Double> stages
) {
}
