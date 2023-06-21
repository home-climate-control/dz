package net.sf.dz3.runtime.config.quarkus.protocol.mqtt;

import net.sf.dz3.runtime.config.quarkus.SensorConfigProvider;
import net.sf.dz3.runtime.config.quarkus.SwitchConfigProvider;

public interface MqttGateway extends MqttBrokerSpec, SensorConfigProvider, SwitchConfigProvider {
}
