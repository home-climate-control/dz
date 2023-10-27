package net.sf.dz3r.runtime.config.quarkus.protocol.mqtt;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.sf.dz3r.runtime.config.quarkus.hardware.SensorConfig;
import net.sf.dz3r.runtime.config.quarkus.hardware.SwitchConfig;

import java.util.Set;

/**
 * Configuration entry for MQTT devices.
 *
 * VT: NOTE: Methods doulbe the superinterface methods because otherwise Jackson doesn't see them on the way out.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public interface MqttDeviceConfig extends MqttGateway {

    @JsonProperty("broker")
    @Override
    MqttBrokerConfig broker();

    @JsonProperty("sensors")
    @Override
    Set<SensorConfig> sensors();

    @JsonProperty("switches")
    @Override
    Set<SwitchConfig> switches();

    @JsonProperty("fans")
    @Override
    Set<FanConfig> fans();
}
