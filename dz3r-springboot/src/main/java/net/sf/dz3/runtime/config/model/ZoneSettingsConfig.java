package net.sf.dz3.runtime.config.model;

public record ZoneSettingsConfig(
        Boolean enabled,
        Double setpoint,
        RangeConfig setpointRange,
        Boolean voting,
        boolean hold,
        Integer dumpPriority
) {
}
