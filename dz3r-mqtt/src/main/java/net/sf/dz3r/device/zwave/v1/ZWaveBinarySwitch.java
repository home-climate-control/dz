package net.sf.dz3r.device.zwave.v1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.dz3r.device.mqtt.v1.AbstractMqttSwitch;
import net.sf.dz3r.device.mqtt.v1.MqttEndpoint;
import net.sf.dz3r.device.mqtt.v1.MqttMessageAddress;
import net.sf.dz3r.signal.Signal;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.scheduler.Scheduler;

import java.time.Instant;
import java.util.Map;

/**
 * Implementation for Z-Wave Binary Switch Generic Device Class over MQTT.
 *
 * See <a href="https://github.com/home-climate-control/dz/issues/235">#235</a> for details.
 */
public class ZWaveBinarySwitch extends AbstractMqttSwitch {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String deviceRootTopic;

    protected ZWaveBinarySwitch(String host, String deviceRootTopic) {
        this(host, MqttEndpoint.DEFAULT_PORT, null, null, false, deviceRootTopic, null);
    }

    protected ZWaveBinarySwitch(String host, int port,
                                String username, String password,
                                boolean reconnect,
                                String deviceRootTopic,
                                Scheduler scheduler) {
        super(new MqttMessageAddress(new MqttEndpoint(host, port), deviceRootTopic), username, password, reconnect, scheduler);

        this.deviceRootTopic = deviceRootTopic;

        // Z-Wave JS UI produces multiple MQTT messages per targetValue/set message, must drain them proactively
        // or we'll run out of sync

        getStateFlux()
                .doOnNext(signal -> logger.debug("async: {}", signal))
                .map(this::getState)
                .doOnNext(state -> lastKnownState = state)
                .subscribe();
    }

    @Override
    protected String getGetStateTopic() {
        return getCurrentValueTopic();
    }

    @Override
    protected String getSetStateTopic() {
        return getTargetValueSetTopic();
    }

    @Override
    protected String renderPayload(boolean state) {
        return "{\"value\": " + state + "}";
    }

    @Override
    protected Signal<Boolean, Void> parsePayload(String message) {
        ThreadContext.push("parseState");
        try {

        var payload = objectMapper.readValue(message, Map.class);

        logger.debug("payload: {}", payload);

        var timestamp = Instant.ofEpochMilli(Long.parseLong(payload.get("time").toString()));
        var state = Boolean.valueOf(payload.get("value").toString());

        return new Signal<>(timestamp, state);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Can't parse JSON: " + message, e);
        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Z-Wave gateway specific "get state" topic name.
     */
    private String getCurrentValueTopic() {
        return deviceRootTopic + "/37/0/currentValue";
    }

    /**
     * Z-Wave gateway specific "set state" topic name.
     */
    private String getTargetValueSetTopic() {
        return deviceRootTopic + "/37/0/targetValue/set";
    }
}
