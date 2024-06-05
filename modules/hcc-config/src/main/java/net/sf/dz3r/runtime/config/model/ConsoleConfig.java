package net.sf.dz3r.runtime.config.model;

import java.util.Set;

public record ConsoleConfig(
        Set<String> directors,
        Set<String> sensors
) {
}
