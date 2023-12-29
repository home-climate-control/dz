package net.sf.dz3r.runtime.config;

import net.sf.dz3r.runtime.config.hardware.SensorConfig;

import java.util.Set;

public interface SensorConfigProvider {
    Set<SensorConfig> sensors();
}
