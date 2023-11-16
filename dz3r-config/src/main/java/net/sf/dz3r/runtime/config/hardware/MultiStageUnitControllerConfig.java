package net.sf.dz3r.runtime.config.hardware;

import net.sf.dz3r.runtime.config.Identifiable;

import java.util.List;

public record MultiStageUnitControllerConfig(
        String id,
        List<Double> stages
) implements Identifiable {
}
