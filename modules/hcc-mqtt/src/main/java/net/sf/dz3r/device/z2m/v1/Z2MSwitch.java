package net.sf.dz3r.device.z2m.v1;

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
 * Implementation for a Zigbee switch over <a href="https://zigbee2mqtt.io">Zigbee2MQTT</a>.
 *
 * @see net.sf.dz3r.device.esphome.v1.ESPHomeSwitch
 * @see net.sf.dz3r.device.zwave.v1.ZWaveBinarySwitch
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 *
 * @deprecated Use {@link net.sf.dz3r.device.z2m.v2.Z2MCqrsSwitch} instead.
 */
@Deprecated(since = "5.0.0")
public class Z2MSwitch extends AbstractMqttSwitch {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String deviceRootTopic;

    /**
     * Create an instance.
     *
     * Even though deprecated, left intact not to disrupt existing configurations until
     * <a href="https://github.com/home-climate-control/dz/issues/47">issue 47</a> is complete.
     *
     * @deprecated Use {@link Z2MSwitch#Z2MSwitch(MqttAdapter, String, boolean, Scheduler)} instead.
     */
    @Deprecated(forRemoval = false)
    public Z2MSwitch(String host, String deviceRootTopic) {
        this(host, MqttEndpoint.DEFAULT_PORT, null, null, false, deviceRootTopic, false, null);
    }

    /**
     * Create an instance.
     *
     * Even though deprecated, left intact not to disrupt existing configurations until
     * <a href="https://github.com/home-climate-control/dz/issues/47">issue 47</a> is complete.
     *
     * @deprecated Use {@link Z2MSwitch#Z2MSwitch(MqttAdapter, String, boolean, Scheduler)} instead.
     */
    @Deprecated(forRemoval = false)
    public Z2MSwitch(String host, int port,
                     String username, String password,
                     boolean reconnect,
                     String deviceRootTopic) {
        this(host, port, username, password, reconnect, deviceRootTopic, false, null);
    }

    /**
     * Create an instance.
     *
     * Even though deprecated, left intact not to disrupt existing configurations until
     * <a href="https://github.com/home-climate-control/dz/issues/47">issue 47</a> is complete.
     *
     * @deprecated Use {@link Z2MSwitch#Z2MSwitch(MqttAdapter, String, boolean, Scheduler)} instead.
     */
    @Deprecated(forRemoval = false)
    public Z2MSwitch(String host, int port,
                     String username, String password,
                     boolean reconnect,
                     String deviceRootTopic,
                     boolean optimistic,
                     Scheduler scheduler) {

        this(
                new MqttAdapterImpl(new MqttEndpoint(host, port), username, password, reconnect, DEFAULT_CACHE_AGE),
                deviceRootTopic,
                optimistic,
                scheduler);
    }

    public Z2MSwitch(
            MqttAdapter mqttAdapter,
            String deviceRootTopic,
            boolean optimistic,
            Scheduler scheduler) {

        // Zigbee seems to suffer from buffer overflow; let's not allow to pound it more often than once in 30 seconds
        super(
                mqttAdapter,
                new MqttMessageAddress(
                        mqttAdapter.getAddress(), deviceRootTopic),
                scheduler,
                Duration.ofSeconds(30),
                optimistic,
                null);

        this.deviceRootTopic = deviceRootTopic;

        if (optimistic) {
            logger.warn("{}: configured optimistic, you must realize the risks", getAddress());
        }

        // VT: NOTE: Do we need to sync here like we do in Z-Wave?

        getStateFlux()
                .doOnNext(signal -> logger.debug("async: {}", signal))
                .map(this::getState)
                .doOnNext(state -> lastKnownState = state)
                .subscribe();
    }

    @Override
    protected boolean includeSubtopics() {
        return false;
    }

    @Override
    protected String getGetStateTopic() {
        // Z2M pushes device state as JSON in the device root topic
        return deviceRootTopic;
    }

    @Override
    protected String getSetStateTopic() {
        return deviceRootTopic + "/set";
    }

    @Override
    protected String renderPayload(boolean state) {
        return "{\"state\": \"" + (state ? "ON" : "OFF") + "\"}";
    }

    @Override
    protected Signal<Boolean, Void> parsePayload(String message) {
        ThreadContext.push("parseState");
        try {

            var payload = objectMapper.readValue(message, Map.class);

            logger.debug("payload: {}", payload);

            // There is no timestamp in Z2M message payload
            var timestamp = Instant.now();
            var stateString = String.valueOf(payload.get("state"));
            boolean state = switch (stateString) {
                case "OFF" -> false;
                case "ON" -> true;
                default -> throw new IllegalArgumentException("Don't know how to parse state out of: " + message);
            };

            return new Signal<>(timestamp, state);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Can't parse JSON: " + message, e);
        } finally {
            ThreadContext.pop();
        }
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
