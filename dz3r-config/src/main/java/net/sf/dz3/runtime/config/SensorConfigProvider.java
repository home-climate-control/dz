package net.sf.dz3.runtime.config;

import net.sf.dz3.runtime.config.hardware.SensorConfig;

import java.util.List;

public interface SensorConfigProvider {
    List<SensorConfig> sensors();
}
