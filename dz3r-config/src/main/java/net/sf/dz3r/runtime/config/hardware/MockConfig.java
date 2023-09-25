package net.sf.dz3r.runtime.config.hardware;

import java.util.Set;

public record MockConfig(
        Set<SwitchConfig> switches
) {
}
