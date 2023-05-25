package net.sf.dz3.runtime.config;

import net.sf.dz3.runtime.config.hardware.SwitchConfig;

import java.util.List;

public interface SwitchConfigProvider {
    List<SwitchConfig> switches();
}
