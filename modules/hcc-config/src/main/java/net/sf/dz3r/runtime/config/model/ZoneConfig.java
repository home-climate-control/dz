package net.sf.dz3r.runtime.config.model;

import net.sf.dz3r.model.Zone;
import net.sf.dz3r.runtime.config.Identifiable;

/**
 * {@link Zone configuration in Spring notation (as a record).
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2024
 */
public record ZoneConfig(
        String id,
        String name,
        PidControllerConfig controller,
        HalfLifeConfig sensitivity,
        ZoneSettingsConfig settings,
        EconomizerConfig economizer
) implements Identifiable {
}
