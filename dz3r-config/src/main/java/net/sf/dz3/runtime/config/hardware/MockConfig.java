package net.sf.dz3.runtime.config.hardware;

import java.util.Set;

public record MockConfig(
        Set<SwitchConfig> switches
) {
}
