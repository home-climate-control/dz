package net.sf.dz3r.runtime.config.model;

public record ZoneConfig(
        String id,
        String name,
        PidControllerConfig controller,
        ZoneSettingsConfig settings,
        EconomizerConfig economizer
) {
}
