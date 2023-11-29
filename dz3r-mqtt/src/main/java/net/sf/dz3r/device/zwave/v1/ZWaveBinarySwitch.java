package net.sf.dz3r.device.zwave.v1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import net.sf.dz3r.device.mqtt.MqttAdapter;
import net.sf.dz3r.device.mqtt.v1.AbstractMqttSwitch;
import net.sf.dz3r.device.mqtt.v1.MqttEndpoint;
import net.sf.dz3r.device.mqtt.v1.MqttMessageAddress;
import net.sf.dz3r.device.mqtt.v2async.MqttAdapterImpl;
import net.sf.dz3r.signal.Signal;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.scheduler.Scheduler;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static net.sf.dz3r.device.mqtt.v2.AbstractMqttListener.DEFAULT_CACHE_AGE;

/**
 * Implementation for Z-Wave Binary Switch Generic Device Class over MQTT.
 *
 * See <a href="https://github.com/home-climate-control/dz/issues/235">#235</a> for details.
 *
 * @see net.sf.dz3r.device.esphome.v1.ESPHomeSwitch
 * @see net.sf.dz3r.device.z2m.v1.Z2MSwitch
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 *
 * @deprecated Use {@link net.sf.dz3r.device.zwave.v2.ZWaveCqrsBinarySwitch} instead.
 */
@Deprecated(since = "5.0.0")
public class ZWaveBinarySwitch extends AbstractMqttSwitch {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String deviceRootTopic;

    /**
     * Create an instance.
     *
     * Even though deprecated, left intact not to disrupt existing configurations until
     * <a href="https://github.com/home-climate-control/dz/issues/47">issue 47</a> is complete.
     *
     * @deprecated Use {@link ZWaveBinarySwitch#ZWaveBinarySwitch(MqttAdapter, String, boolean, Scheduler)} instead.
     */
    @Deprecated(forRemoval = false)
    public ZWaveBinarySwitch(String host, String deviceRootTopic) {
        this(host, MqttEndpoint.DEFAULT_PORT, null, null, false, deviceRootTopic, null);
    }

    /**
     * Create an instance.
     *
     * Even though deprecated, left intact not to disrupt existing configurations until
     * <a href="https://github.com/home-climate-control/dz/issues/47">issue 47</a> is complete.
     *
     * @deprecated Use {@link ZWaveBinarySwitch#ZWaveBinarySwitch(MqttAdapter, String, boolean, Scheduler)} instead.
     */
    @Deprecated(forRemoval = false)
    public ZWaveBinarySwitch(String host, int port,
                             String username, String password,
                             boolean reconnect,
                             String deviceRootTopic) {
        this(host, port, username, password, reconnect, deviceRootTopic, null);
    }

    /**
     * @deprecated Use {@link ZWaveBinarySwitch#ZWaveBinarySwitch(MqttAdapter, String, boolean, Scheduler)} instead.
     */
    @Deprecated(forRemoval = false)
    public ZWaveBinarySwitch(String host, int port,
                             String username, String password,
                             boolean reconnect,
                             String deviceRootTopic,
                             Scheduler scheduler) {

        this(
                new MqttAdapterImpl(new MqttEndpoint(host, port), username, password, reconnect, DEFAULT_CACHE_AGE),
                deviceRootTopic,
                false,
                scheduler);
    }

    public ZWaveBinarySwitch(
            MqttAdapter mqttAdapter,
            String deviceRootTopic,
            boolean optimistic,
            Scheduler scheduler) {

        // Z-Wave seems to suffer from buffer overflow; let's not allow to pound it more often than once in 30 seconds
        super(
                mqttAdapter,
                new MqttMessageAddress(
                        mqttAdapter.getAddress(),
                        deviceRootTopic),
                scheduler,
                Duration.ofSeconds(30),
                optimistic, null);

        this.deviceRootTopic = deviceRootTopic;

        if (optimistic) {
            logger.warn("{}: configured optimistic, you must realize the risks", getAddress());
        }

        // Z-Wave JS UI produces multiple MQTT messages per targetValue/set message, must drain them proactively
        // or we'll run out of sync

        getStateFlux()
                .doOnNext(signal -> logger.debug("async: {}", signal))
                .map(this::getState)
                .doOnNext(state -> lastKnownState = state)
                .subscribe();
    }

    @Override
    protected boolean includeSubtopics() {
        return true;
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

    @Override
    protected void setStateSync(boolean state) throws IOException {

        // This should make sure the MQTT broker connection is alive
        getStateFlux();

        // VT: NOTE: This message will generate a flurry of MQTT state notification - the number is indeterminate,
        // sometimes three, sometimes four.
        mqttAdapter.publish(
                getSetStateTopic(),
                renderPayload(state),
                MqttQos.AT_LEAST_ONCE,
                false);

        // Due to the note above, can't just read one message and expect the value to be the same (this fails).
        // Need to drain the stream and return the last known state.

        // Force obtaining a fresh value with next getStateSync()
        lastKnownState = null;
    }
}
