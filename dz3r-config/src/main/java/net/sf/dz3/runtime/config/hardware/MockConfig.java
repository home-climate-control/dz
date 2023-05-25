package net.sf.dz3.runtime.config.hardware;

import java.util.List;

public record MockConfig(
        List<SwitchConfig> switches
) {
}
