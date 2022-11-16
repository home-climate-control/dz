package net.sf.dz3r.device.esphome.v1;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import net.sf.dz3r.device.mqtt.v1.AbstractMqttSwitch;
import net.sf.dz3r.device.mqtt.v1.MqttEndpoint;
import net.sf.dz3r.device.mqtt.v1.MqttMessageAddress;
import net.sf.dz3r.signal.Signal;
import reactor.core.scheduler.Scheduler;

import java.io.IOException;
import java.time.Instant;

/**
 * Implementation to control <a href="https://esphome.io/components/switch/">ESPHome Switch Component</a> via
 * <a href="https://esphome.io/components/mqtt">ESPHome MQTT Client Component</a>.
 *
 * See <a href="https://github.com/home-climate-control/dz/wiki/HOWTO:-DZ-to-ESPHome-integration">HOWTO: DZ to ESPHome integration</a>
 * for configuration details.
 *
 * @see net.sf.dz3r.device.zwave.v1.ZWaveBinarySwitch
 * @see net.sf.dz3r.device.z2m.v1.Z2MSwitch
 */
public class ESPHomeSwitch extends AbstractMqttSwitch {

    private final String deviceRootTopic;

    /**
     * Create an unauthenticated instance with no reconnect and default scheduler.
     *
     * @param host MQTT broker host.
     * @param deviceRootTopic Switch root topic. See the doc link at the top for the configuration reference.
     *
     */
    protected ESPHomeSwitch(String host, String deviceRootTopic) {
        this(host, MqttEndpoint.DEFAULT_PORT, null, null, false, deviceRootTopic, null);
    }

    /**
     * Create a fully configured instance with default scheduler.
     *
     * @param deviceRootTopic Switch root topic. See the doc link at the top for the configuration reference.
     */
    protected ESPHomeSwitch(String host, int port,
                            String username, String password,
                            boolean reconnect,
                            String deviceRootTopic) {
        this(host, port, username, password, reconnect, deviceRootTopic, null);
    }

    /**
     * Create a fully configured instance.
     *
     * @param deviceRootTopic Switch root topic. See the doc link at the top for the configuration reference.
     */
    protected ESPHomeSwitch(String host, int port,
                            String username, String password,
                            boolean reconnect,
                            String deviceRootTopic,
                            Scheduler scheduler) {

        super(new MqttMessageAddress(new MqttEndpoint(host, port), deviceRootTopic), username, password, reconnect, scheduler);

        this.deviceRootTopic = deviceRootTopic;

        // Must proactively retrieve the state because otherwise it will be lost ("no subscriptions, dropped")
        getStateFlux()
                .doOnNext(signal -> logger.debug("async: {}", signal))
                .map(this::getState)
                .doOnNext(state -> lastKnownState = state)
                .subscribe();
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

        switch (message) {
            case "ON": return new Signal<>(timestamp, Boolean.TRUE);
            case "OFF" : return new Signal<>(timestamp, Boolean.FALSE);

            default:
                logger.warn("malformed payload '{}', returning FALSE", message);
                return new Signal<>(timestamp, Boolean.FALSE);
        }
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
