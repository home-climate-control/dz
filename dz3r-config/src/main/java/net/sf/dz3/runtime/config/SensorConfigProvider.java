package net.sf.dz3.runtime.config;

import net.sf.dz3.runtime.config.hardware.SensorConfig;

import java.util.Set;

public interface SensorConfigProvider {
    Set<SensorConfig> sensors();
}
