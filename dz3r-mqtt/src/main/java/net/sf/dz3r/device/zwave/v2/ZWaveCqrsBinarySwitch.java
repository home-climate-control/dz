package net.sf.dz3r.device.zwave.v2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.dz3r.device.mqtt.v1.MqttAdapter;
import net.sf.dz3r.device.mqtt.v1.MqttSignal;
import net.sf.dz3r.device.mqtt.v2.AbstractMqttCqrsSwitch;
import org.apache.logging.log4j.ThreadContext;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;

public class ZWaveCqrsBinarySwitch extends AbstractMqttCqrsSwitch {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ZWaveCqrsBinarySwitch(
            String id,
            Clock clock,
            Duration heartbeat,
            Duration pace,
            MqttAdapter mqttAdapter, String rootTopic) {
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

            actual = Boolean.valueOf(payload.get("value").toString());

        } catch (JsonProcessingException e) {

            actual = null;
            throw new IllegalStateException("Can't parse JSON: " + message, e);

        } finally {
            ThreadContext.pop();
        }
    }

    @Override
    protected boolean includeSubtopics() {
        return true;
    }

    @Override
    protected String getAvailabilityTopic() {
        return rootTopic + "/status";
    }

    @Override
    public boolean isAvailable() {
        // Check for "Alive" or "Dead" topic payload, otherwise log error and return false

        if (availabilityMessage == null) {
            return false;
        }

        if (availabilityMessage.toLowerCase().contains("alive")) {
            return true;
        }

        if (availabilityMessage.toLowerCase().contains("dead")) {
            return false;
        }

        logger.warn("{}: unknown availability message: {}", getAddress(), availabilityMessage);
        return false;
    }

    /**
     * Z-Wave gateway specific "get state" topic name.
     */
    @Override
    protected String getStateTopic() {
        return rootTopic + "/37/0/currentValue";
    }

    /**
     * Z-Wave gateway specific "set state" topic name.
     */
    @Override
    protected String getCommandTopic() {
        return rootTopic + "/37/0/targetValue/set";
    }

    @Override
    protected String renderPayload(Boolean state) {
        return "{\"value\": " + state + "}";
    }
}
