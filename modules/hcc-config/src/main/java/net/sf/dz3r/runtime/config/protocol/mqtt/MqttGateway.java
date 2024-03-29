package net.sf.dz3r.runtime.config.protocol.mqtt;

import net.sf.dz3r.runtime.config.FanConfigProvider;
import net.sf.dz3r.runtime.config.SensorConfigProvider;
import net.sf.dz3r.runtime.config.SwitchConfigProvider;

public interface MqttGateway extends SensorConfigProvider, SwitchConfigProvider, FanConfigProvider {
    MqttBrokerSpec broker();
}
