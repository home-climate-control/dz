package net.sf.dz3r.runtime.config.model;

import net.sf.dz3r.runtime.config.Identifiable;

public record ZoneConfig(
        String id,
        String name,
        PidControllerConfig controller,
        ZoneSettingsConfig settings,
        EconomizerConfig economizer
) implements Identifiable {
}
