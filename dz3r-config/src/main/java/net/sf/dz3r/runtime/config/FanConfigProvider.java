package net.sf.dz3r.runtime.config;

import net.sf.dz3r.runtime.config.protocol.mqtt.FanConfig;

import java.util.Set;

public interface FanConfigProvider {
    Set<FanConfig> fans();
}
