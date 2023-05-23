package net.sf.dz3.runtime.config.model;

public record ZoneConfig(
        String zoneId,
        String name,
        PidControllerConfig controller,
        ZoneSettingsConfig settings,
        EconomizerConfig economizer
) {
}
