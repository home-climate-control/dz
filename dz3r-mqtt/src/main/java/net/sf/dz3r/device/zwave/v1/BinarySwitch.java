package net.sf.dz3r.device.zwave.v1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import net.sf.dz3r.device.actuator.AbstractSwitch;
import net.sf.dz3r.device.mqtt.v1.MqttAdapter;
import net.sf.dz3r.device.mqtt.v1.MqttEndpoint;
import net.sf.dz3r.device.mqtt.v1.MqttMessageAddress;
import net.sf.dz3r.device.mqtt.v1.MqttSignal;
import net.sf.dz3r.signal.Signal;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation for Z-Wave Binary Switch Generic Device Class over MQTT.
 *
 * See <a href="https://github.com/home-climate-control/dz/issues/235">#235</a> for details.
 */
public class BinarySwitch extends AbstractSwitch<MqttMessageAddress> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final MqttAdapter mqttAdapter;

    private final String deviceRootTopic;

    private Flux<MqttSignal> mqttCurrentValueFlux;

    private Signal<Boolean, Void> lastKnownState;

    protected BinarySwitch(String host, String deviceRootTopic) {
        this(host, MqttEndpoint.DEFAULT_PORT, null, null, false, deviceRootTopic, null);
    }

    protected BinarySwitch(String host, int port,
                           String username, String password,
                           boolean reconnect,
                           String deviceRootTopic,
                           Scheduler scheduler) {
        super(new MqttMessageAddress(new MqttEndpoint(host, port), deviceRootTopic), scheduler);

        mqttAdapter = new MqttAdapter(getAddress().endpoint, username, password, reconnect);
        this.deviceRootTopic = deviceRootTopic;

        // Z-Wave JS UI produces multiple MQTT messages per targetValue/set message, must drain them proactively
        // or we'll run out of sync

        getCurrentValueFlux()
                .doOnNext(signal -> logger.debug("async: {}", signal))
                .map(this::getState)
                .doOnNext(state -> lastKnownState = state)
                .subscribe();
    }

    private String getCurrentValueTopic() {
        return deviceRootTopic + "/37/0/currentValue";
    }

    private String getTargetValueSetTopic() {
        return deviceRootTopic + "/37/0/targetValue/set";
    }

    @Override
    protected void setStateSync(boolean state) throws IOException {

        // This should make sure the MQTT broker connection is alive
        getCurrentValueFlux();

        // VT: NOTE: This message will generate a flurry of MQTT state notification - the number is indeterminate,
        // sometimes three, sometimes four.
        mqttAdapter.publish(
                getTargetValueSetTopic(),
                "{\"value\": " + state + "}",
                MqttQos.AT_LEAST_ONCE,
                false);

        // Due to the note above, can't just read one message and expect the value to be the same (this fails).
        // Need to drain the stream and return the last known state.
        getStateSync(true);
    }

    @Override
    protected boolean getStateSync() throws IOException {
        return getStateSync(false);
    }

    /**
     * Read, store, and return the hardware switch value.
     *
     * @param flushCache {@code true} if the currently known value must be discarded and new value needs to be awaited.
     */
    private boolean getStateSync(boolean flushCache) {

        ThreadContext.push("getStateSync" + (flushCache ? "+flush" : ""));

        try {

            if (flushCache) {
                lastKnownState = null;
            }

            if (lastKnownState != null) {
                logger.debug("returning cached state: {}", lastKnownState);
                return lastKnownState.getValue();
            }

            logger.debug("Awaiting new state for {}...", getCurrentValueTopic());

            lastKnownState = getCurrentValueFlux()
                    .map(this::getState)
                    .blockFirst();

            // Sonar is paranoid, reports blockFirst() can return null. That's a pretty bizarre corner case, but OK.

            logger.debug("signal @{}: {}",
                    Optional.ofNullable(lastKnownState)
                            .map(state -> state.timestamp.atZone(ZoneId.systemDefault()))
                            .orElse(null),
                    lastKnownState);

            return Optional.ofNullable(lastKnownState)
                    .map(Signal::getValue)
                    .orElseThrow(() -> new IllegalStateException("null lastKnownState - something is seriously wrong"));

        } finally {
            ThreadContext.pop();
        }
    }

    private Signal<Boolean, Void> getState(MqttSignal mqttSignal) {

        ThreadContext.push("getState");
        try {

            logger.debug("getState: {}", mqttSignal);

            var payload = objectMapper.readValue(mqttSignal.message, Map.class);

            logger.debug("payload: {}", payload);

            var timestamp = Instant.ofEpochMilli(Long.parseLong(payload.get("time").toString()));
            var state = Boolean.valueOf(payload.get("value").toString());

            return new Signal<>(timestamp, state);

        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Can't parse JSON: " + mqttSignal.message, e);
        } finally {
            ThreadContext.pop();
        }
    }

    private synchronized Flux<MqttSignal> getCurrentValueFlux() {

        if (mqttCurrentValueFlux != null) {
            return mqttCurrentValueFlux;
        }

        mqttCurrentValueFlux = mqttAdapter.getFlux(getCurrentValueTopic());

        return mqttCurrentValueFlux;
    }
}