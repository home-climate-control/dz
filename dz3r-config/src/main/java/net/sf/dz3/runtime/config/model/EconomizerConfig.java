package net.sf.dz3.runtime.config.model;

import net.sf.dz3r.model.HvacMode;

public record EconomizerConfig(
        String ambientSensor,
        double changeoverDelta,
        double targetTemperature,
        Boolean keepHvacOn,
        PidControllerConfig controller,
        HvacMode mode,
        String switchAddress
) {
}
