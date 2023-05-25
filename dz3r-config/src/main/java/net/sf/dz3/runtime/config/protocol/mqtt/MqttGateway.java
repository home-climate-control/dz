package net.sf.dz3.runtime.config.protocol.mqtt;

import net.sf.dz3.runtime.config.SensorConfigProvider;
import net.sf.dz3.runtime.config.SwitchConfigProvider;

public interface MqttGateway extends MqttBrokerSpec, SensorConfigProvider, SwitchConfigProvider {
}
