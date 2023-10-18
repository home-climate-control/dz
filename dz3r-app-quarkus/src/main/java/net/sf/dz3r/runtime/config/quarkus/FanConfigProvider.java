package net.sf.dz3r.runtime.config.quarkus;

import net.sf.dz3r.runtime.config.quarkus.protocol.mqtt.FanConfig;

import java.util.Set;

public interface FanConfigProvider {
    Set<FanConfig> fans();
}
