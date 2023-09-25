package net.sf.dz3r.runtime.config;

import net.sf.dz3r.runtime.config.hardware.SwitchConfig;

import java.util.Set;

public interface SwitchConfigProvider {
    Set<SwitchConfig> switches();
}
