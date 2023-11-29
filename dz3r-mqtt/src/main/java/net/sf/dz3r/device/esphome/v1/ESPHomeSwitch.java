package net.sf.dz3r.device.esphome.v1;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import net.sf.dz3r.device.mqtt.MqttAdapter;
import net.sf.dz3r.device.mqtt.v1.AbstractMqttSwitch;
import net.sf.dz3r.device.mqtt.v1.MqttEndpoint;
import net.sf.dz3r.device.mqtt.v1.MqttMessageAddress;
import net.sf.dz3r.device.mqtt.v2async.MqttAdapterImpl;
import net.sf.dz3r.signal.Signal;
import reactor.core.scheduler.Scheduler;

import java.io.IOException;
import java.time.Instant;

import static net.sf.dz3r.device.mqtt.v2.AbstractMqttListener.DEFAULT_CACHE_AGE;

/**
 * Implementation to control <a href="https://esphome.io/components/switch/">ESPHome Switch Component</a> via
 * <a href="https://esphome.io/components/mqtt">ESPHome MQTT Client Component</a>.
 *
 * See <a href="https://github.com/home-climate-control/dz/wiki/HOWTO:-DZ-to-ESPHome-integration">HOWTO: DZ to ESPHome integration</a>
 * for configuration details.
 *
 * @see net.sf.dz3r.device.zwave.v1.ZWaveBinarySwitch
 * @see net.sf.dz3r.device.z2m.v1.Z2MSwitch
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 *
 * @deprecated Use {@link net.sf.dz3r.device.esphome.v2.ESPHomeCqrsSwitch} instead.
 */
@Deprecated(since = "5.0.0")
public class ESPHomeSwitch extends AbstractMqttSwitch {

    private final String deviceRootTopic;

    /**
     * Create an unauthenticated instance with no reconnect and default scheduler.
     *
     * @param host MQTT broker host.
     * @param deviceRootTopic Switch root topic. See the doc link at the top for the configuration reference.
     *
     */
    public ESPHomeSwitch(String host, String deviceRootTopic) {
        this(host, MqttEndpoint.DEFAULT_PORT, null, null, false, deviceRootTopic, true, null);
    }

    /**
     * Create a fully configured instance with default scheduler.
     *
     * Even though deprecated, left intact not to disrupt existing configurations until
     * <a href="https://github.com/home-climate-control/dz/issues/47">issue 47</a> is complete.
     *
     * @param deviceRootTopic Switch root topic. See the doc link at the top for the configuration reference.
     *
     * @deprecated Use {@link ESPHomeSwitch#ESPHomeSwitch(MqttAdapter, String, boolean, Scheduler)} instead.
     */
    @Deprecated(forRemoval = false)
    public ESPHomeSwitch(String host, int port,
                            String username, String password,
                            boolean reconnect,
                            String deviceRootTopic) {
        this(host, port, username, password, reconnect, deviceRootTopic, true, null);
    }

    /**
     * Create a fully configured instance.
     *
     * Even though deprecated, left intact not to disrupt existing configurations until
     * <a href="https://github.com/home-climate-control/dz/issues/47">issue 47</a> is complete.
     *
     * @param deviceRootTopic Switch root topic. See the doc link at the top for the configuration reference.
     *
     * @deprecated Use {@link ESPHomeSwitch#ESPHomeSwitch(MqttAdapter, String, boolean, Scheduler)} instead.
     */
    @Deprecated(forRemoval = false)
    public ESPHomeSwitch(String host, int port,
                            String username, String password,
                            boolean reconnect,
                            String deviceRootTopic,
                            boolean optimistic,
                            Scheduler scheduler) {

        // VT: NOTE: ESPHome appears to not suffer from buffer overruns like Zigbee and Z-Wave do,
        // so not providing the delay
        this(new MqttAdapterImpl(new MqttEndpoint(host, port), username, password, reconnect, DEFAULT_CACHE_AGE),
                deviceRootTopic,
                optimistic,
                scheduler);
    }

    /**
     *
     * Create a fully configured instance.
     */
    public ESPHomeSwitch(
            MqttAdapter mqttAdapter,
            String deviceRootTopic,
            boolean optimistic,
            Scheduler scheduler) {

        // VT: NOTE: ESPHome appears to not suffer from buffer overruns like Zigbee and Z-Wave do,
        // so not providing the delay
        super(
                mqttAdapter,
                new MqttMessageAddress(
                        mqttAdapter.getAddress(),
                        deviceRootTopic),
                scheduler,
                null,
                optimistic, null);

        this.deviceRootTopic = deviceRootTopic;

        if (!optimistic) {
            // https://github.com/home-climate-control/dz/issues/280
            logger.error("{}: NOT configured optimistic, adjust that when it gets stuck", getAddress());
        }

        // Must proactively retrieve the state because otherwise it will be lost ("no subscriptions, dropped")
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
        return deviceRootTopic + "/state";
    }

    @Override
    protected String getSetStateTopic() {
        return deviceRootTopic + "/command";
    }

    @Override
    protected String renderPayload(boolean state) {
        return state ? "ON" : "OFF";
    }

    @Override
    protected Signal<Boolean, Void> parsePayload(String message) {

        // No timestamp in the MQTT message, must use real time
        var timestamp = Instant.now();

        return switch (message) {
            case "ON" -> new Signal<>(timestamp, Boolean.TRUE);
            case "OFF" -> new Signal<>(timestamp, Boolean.FALSE);
            default -> {
                logger.warn("malformed payload '{}', returning FALSE", message);
                yield new Signal<>(timestamp, Boolean.FALSE);
            }
        };
    }

    @Override
    protected void setStateSync(boolean state) throws IOException {

        // This should make sure the MQTT broker connection is alive
        getStateFlux();

        mqttAdapter.publish(
                getSetStateTopic(),
                renderPayload(state),
                MqttQos.AT_LEAST_ONCE,
                false);

        // With ESPHome, there will be no MQTT message if the switch didn't change state,
        // so we're going to get stuck forever.

        if (lastKnownState != null && lastKnownState.getValue().equals(state)) {
            logger.debug("state hasn't changed, not waiting for update: {}", lastKnownState);
            return;
        }

        // Force obtaining a fresh value with next getStateSync()
        lastKnownState = null;
    }
}
