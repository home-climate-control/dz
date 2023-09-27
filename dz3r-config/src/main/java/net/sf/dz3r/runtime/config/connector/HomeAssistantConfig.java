package net.sf.dz3r.runtime.config.connector;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import net.sf.dz3r.runtime.config.protocol.mqtt.MqttBrokerConfig;

/**
 * Home Assistant MQTT Discovery configuration.
 *
 * See <a href="https://www.home-assistant.io/integrations/mqtt#mqtt-discovery">MQTT Discovery</a> for more information.
 *
 * Since {@link #discoveryPrefix} is semantically the same as {@link MqttBrokerConfig#rootTopic()},
 * and since Quarkus is picky about nulls but Spring is not, either of them can be used, to the same effect. The parser will
 * enforce valid configuration.
 *
 * @param broker MQTT broker configuration.
 * @param discoveryPrefix Optional, defaults to {@code homeassistant} (no leading slash).
 * @param nodeId Optional, not used by HA. Defaults to {@link net.sf.dz3r.runtime.config.HccRawConfig#instance()}.
 */
@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record HomeAssistantConfig(
        MqttBrokerConfig broker,
        String discoveryPrefix,
        String nodeId
) {
}
