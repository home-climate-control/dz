package net.sf.dz3r.runtime.config.connector;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import net.sf.dz3r.runtime.config.Identifiable;
import net.sf.dz3r.runtime.config.protocol.mqtt.MqttBrokerConfig;

import java.util.Set;

/**
 * Home Assistant MQTT Discovery configuration.
 *
 * See <a href="https://www.home-assistant.io/integrations/mqtt#mqtt-discovery">MQTT Discovery</a> for more information.
 *
 * Since {@link #discoveryPrefix} is semantically the same as {@link MqttBrokerConfig#rootTopic()},
 * and since Quarkus is picky about nulls but Spring is not, either of them can be used, to the same effect. The parser will
 * enforce valid configuration.
 *
 * Also see {@link HomeAssistantConfigParser#parse()} - that's the way to get the well-formed configuration for this object.
 *
 * @param id Identifier, mandatory.
 * @param broker MQTT broker configuration.
 * @param discoveryPrefix Optional, defaults to {@code homeassistant} (no leading slash).
 * @param zones Zones to expose to Home Assistant.
 */
@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record HomeAssistantConfig(
        String id,
        MqttBrokerConfig broker,
        String discoveryPrefix,
        Set<String> zones
) implements HomeAssistantConfigParser, Identifiable {
}
