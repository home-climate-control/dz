package net.sf.dz3r.runtime.config.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import net.sf.dz3r.model.HvacMode;

import java.time.Duration;

@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record EconomizerConfig(
        String ambientSensor,
        PidControllerConfig controller,
        HvacMode mode,
        String hvacDevice,
        Duration timeout,
        Settings settings
) {
    public record Settings(
            double changeoverDelta,
            double targetTemperature,
            Boolean keepHvacOn
    ) {

    }
}
