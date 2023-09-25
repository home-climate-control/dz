package net.sf.dz3r.runtime.config.quarkus;

import net.sf.dz3r.runtime.config.quarkus.hardware.SensorConfig;

import java.util.Set;

public interface SensorConfigProvider {
    Set<SensorConfig> sensors();
}
