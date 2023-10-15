package net.sf.dz3r.runtime.config.protocol.mqtt;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import net.sf.dz3r.runtime.config.hardware.SensorConfig;
import net.sf.dz3r.runtime.config.hardware.SwitchConfig;

import java.util.Set;

/**
 * Configuration entry for MQTT based devices.
 *
 * @param broker MQTT broker configuration
 * @param sensors Set of sensors, optional.
 * @param switches Set of switches, optional.
 */
@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record MqttDeviceConfig(
        MqttBrokerConfig broker,
        Set<SensorConfig> sensors,
        Set<SwitchConfig> switches,
        Set<FanConfig> fans
) implements MqttGateway {
}
