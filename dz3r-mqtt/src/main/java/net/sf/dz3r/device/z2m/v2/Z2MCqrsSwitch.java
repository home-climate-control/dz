package net.sf.dz3r.device.z2m.v2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import net.sf.dz3r.device.mqtt.v1.MqttAdapter;
import net.sf.dz3r.device.mqtt.v1.MqttMessageAddress;
import net.sf.dz3r.device.mqtt.v1.MqttSignal;
import net.sf.dz3r.device.mqtt.v2.AbstractMqttCqrsSwitch;
import net.sf.dz3r.instrumentation.Marker;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.Disposable;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;

/**
 * Implementation for a Zigbee switch over <a href="https://zigbee2mqtt.io">Zigbee2MQTT</a>.
 *
 * @see net.sf.dz3r.device.esphome.v2.ESPHomeCqrsSwitch
 * @see net.sf.dz3r.device.zwave.v1.ZWaveBinarySwitch
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class Z2MCqrsSwitch extends AbstractMqttCqrsSwitch {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String availabilityMessage;

    private final Disposable availabilityFlux;
    private final Disposable rootFlux;
    protected Z2MCqrsSwitch(
            String id,
            Clock clock,
            Duration heartbeat,
            Duration pace,
            MqttAdapter mqttAdapter,
            MqttMessageAddress address) {
        super(id, clock, heartbeat, pace, mqttAdapter, address);

        availabilityFlux = mqttAdapter
                .getFlux(getAvailabilityTopic(), false)
                .subscribe(this::parseAvailability);
        rootFlux = mqttAdapter
                .getFlux(address.topic, true)
                .subscribe(this::parseState);
    }

    private void parseAvailability(MqttSignal message) {

        this.availabilityMessage = message.message();
        stateSink.tryEmitNext(getStateSignal());
    }

    private void parseState(MqttSignal message) {
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

        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Can't parse JSON: " + message, e);
        } finally {
            ThreadContext.pop();
        }
    }

    @Override
    protected void setStateSync(Boolean command) {

        ThreadContext.push("setStateSync");
        var m = new Marker("setStateSync", Level.TRACE);

        try {
            // VT: NOTE: This message will generate a flurry of MQTT state notifications - the number is indeterminate,
            // sometimes three, sometimes four. Previous implementation cared, we don't.
            mqttAdapter.publish(
                    getCommandTopic(),
                    renderPayload(command),
                    MqttQos.AT_LEAST_ONCE,
                    false);
            queueDepth.decrementAndGet();

        } finally {
            m.close();
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

    private String getAvailabilityTopic() {
        return address.topic + "/availability";
    }

    @Override
    protected String getStateTopic() {
        // Z2M pushes device state as JSON in the device root topic
        return address.topic;
    }

    @Override
    protected String getCommandTopic() {
        return address.topic + "/set";
    }

    @Override
    protected String renderPayload(boolean state) {
        return "{\"state\": \"" + (state ? "ON" : "OFF") + "\"}";
    }

    @Override
    protected void closeSubclass2() {

        // Close the comms channel
        rootFlux.dispose();
        availabilityFlux.dispose();
    }
}
