package net.sf.dz3r.runtime.config.model;

import java.util.Set;

public record ConsoleConfig(
        TemperatureUnit units,
        Set<String> directors,
        Set<String> sensors
) {
}
