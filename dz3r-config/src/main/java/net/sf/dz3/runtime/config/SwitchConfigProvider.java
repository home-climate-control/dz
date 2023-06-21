package net.sf.dz3.runtime.config;

import net.sf.dz3.runtime.config.hardware.SwitchConfig;

import java.util.Set;

public interface SwitchConfigProvider {
    Set<SwitchConfig> switches();
}
