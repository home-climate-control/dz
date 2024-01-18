package net.sf.dz3r.device.z2m.v2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.dz3r.device.mqtt.MqttAdapter;
import net.sf.dz3r.device.mqtt.v1.MqttSignal;
import net.sf.dz3r.device.mqtt.v2.AbstractMqttCqrsSwitch;
import org.apache.logging.log4j.ThreadContext;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;

/**
 * Implementation for a Zigbee switch over <a href="https://zigbee2mqtt.io">Zigbee2MQTT</a>.
 *
 * @see net.sf.dz3r.device.esphome.v2.ESPHomeCqrsSwitch
 * @see net.sf.dz3r.device.zwave.v2.ZWaveCqrsBinarySwitch
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class Z2MCqrsSwitch extends AbstractMqttCqrsSwitch {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Z2MCqrsSwitch(
            String id,
            Clock clock,
            Duration heartbeat,
            Duration pace,
            MqttAdapter mqttAdapter,
            String rootTopic) {
        super(id, clock, heartbeat, pace, mqttAdapter, rootTopic);
    }

    @Override
    protected void parseState(MqttSignal message) {
        ThreadContext.push("parseState");
        try {

            if (!getStateTopic().equals(message.topic())) {
                return;
            }

            var payload = objectMapper.readValue(message.message(), Map.class);

            logger.debug("payload: {}", payload);

            var stateString = String.valueOf(payload.get("state"));
            switch (stateString) {
                case "OFF" -> actual = false;
                case "ON" -> actual = true;
                default -> logger.error("{}: can't parse state from {}", id, message.message());
            }

            stateSink.tryEmitNext(getStateSignal());

        } catch (JsonProcessingException ex) {

            // Throwing an exception here breaks everything
            // https://github.com/home-climate-control/dz/issues/303

            logger.error("Can't parse JSON:\n{}\n---", message, ex);
        } finally {
            ThreadContext.pop();
        }
    }

    @Override
    public boolean isAvailable() {

        // VT: NOTE: per https://www.zigbee2mqtt.io/guide/configuration/device-availability.html,
        // the default timeout is 10 minutes - that'll send the queue depth through the roof, and if THIS is reported
        // as offline, then it is seriously offline. Might need to take measures not to saturate the device queue with
        // obsolete commands - this calls for a "stale command timeout" configuration value.

        return availabilityMessage != null && availabilityMessage.contains("online");
    }

    @Override
    protected boolean includeSubtopics() {
        return false;
    }

    @Override
    protected String getAvailabilityTopic() {
        return rootTopic + "/availability";
    }

    @Override
    protected String getStateTopic() {
        // Z2M pushes device state as JSON in the device root topic
        return rootTopic;
    }

    @Override
    protected String getCommandTopic() {
        return rootTopic + "/set";
    }

    @Override
    protected String renderPayload(Boolean state) {
        return "{\"state\": \"" + (Boolean.TRUE.equals(state) ? "ON" : "OFF") + "\"}";
    }
}
