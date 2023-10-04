package net.sf.dz3r.runtime.config.quarkus.protocol.mqtt;

import net.sf.dz3r.runtime.config.quarkus.SensorConfigProvider;
import net.sf.dz3r.runtime.config.quarkus.SwitchConfigProvider;

public interface MqttGateway extends SensorConfigProvider, SwitchConfigProvider {
    MqttBrokerSpec broker();
}
