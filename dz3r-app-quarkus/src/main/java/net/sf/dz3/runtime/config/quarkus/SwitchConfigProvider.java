package net.sf.dz3.runtime.config.quarkus;

import net.sf.dz3.runtime.config.quarkus.hardware.SwitchConfig;

import java.util.Set;

public interface SwitchConfigProvider {
    Set<SwitchConfig> switches();
}
