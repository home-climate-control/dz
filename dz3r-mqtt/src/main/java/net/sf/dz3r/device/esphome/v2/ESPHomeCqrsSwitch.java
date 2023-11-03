package net.sf.dz3r.device.esphome.v2;

import net.sf.dz3r.common.HCCObjects;
import net.sf.dz3r.device.mqtt.v1.MqttAdapter;
import net.sf.dz3r.device.mqtt.v1.MqttSignal;
import net.sf.dz3r.device.mqtt.v2.AbstractMqttCqrsSwitch;
import org.apache.logging.log4j.ThreadContext;

import java.time.Clock;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Implementation to control <a href="https://esphome.io/components/switch/">ESPHome Switch Component</a> via
 * <a href="https://esphome.io/components/mqtt">ESPHome MQTT Client Component</a>.
 *
 * See <a href="https://github.com/home-climate-control/dz/wiki/HOWTO:-DZ-to-ESPHome-integration">HOWTO: DZ to ESPHome integration</a>
 * for configuration details.
 *
 * @see net.sf.dz3r.device.zwave.v2.ZWaveCqrsBinarySwitch
 * @see net.sf.dz3r.device.z2m.v2.Z2MCqrsSwitch
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class ESPHomeCqrsSwitch extends AbstractMqttCqrsSwitch {

    private final String availabilityTopic;
    public ESPHomeCqrsSwitch(
            String id,
            Clock clock,
            Duration heartbeat,
            Duration pace,
            MqttAdapter adapter,
            String rootTopic,
            String availabilityTopic) {
        super(id, clock, heartbeat, pace, adapter, rootTopic);

        this.availabilityTopic = HCCObjects.requireNonNull(availabilityTopic, "esphome.switches.availability-topic can't be null (id=" + id + ")");
    }

    @Override
    protected boolean includeSubtopics() {
        return true;
    }

    @Override
    protected String getAvailabilityTopic() {
        return availabilityTopic;
    }

    @Override
    protected String getStateTopic() {
        return rootTopic + "/state";
    }

    @Override
    protected String getCommandTopic() {
        return rootTopic + "/command";
    }

    private static final Map<String, String> topic2status = Collections.synchronizedMap(new TreeMap<>());

    @Override
    protected void parseAvailability(MqttSignal message) {

        ThreadContext.push("parseAvailability");

        try {

            logger.trace("{}: availability={}", id, message);

            // https://github.com/home-climate-control/dz/issues/295
            // Until https://github.com/esphome/issues/issues/5030 is fixed, multiple logical ESPHome switches
            // hosted on the same chip will share the same availability topic. Ugly, but we have to share the state.

            topic2status.put(message.topic(), message.message());

            for (var kv : topic2status.entrySet()) {
                logger.trace("  {}={}", kv.getKey(), kv.getValue());
            }

            stateSink.tryEmitNext(getStateSignal());

        } finally {
            ThreadContext.pop();
        }
    }
    @Override
    protected String renderPayload(Boolean state) {
        return Boolean.TRUE.equals(state) ? "ON" : "OFF";
    }

    @Override
    protected void parseState(MqttSignal message) {
        if (!getStateTopic().equals(message.topic())) {
            return;
        }

        switch (message.message()) {
            case "OFF" -> actual = false;
            case "ON" -> actual = true;
            default -> logger.error("{}: can't parse state from {}", id, message);
        }

        stateSink.tryEmitNext(getStateSignal());
    }

    @Override
    public boolean isAvailable() {
        return "online".equals(topic2status.get(getAvailabilityTopic()));
    }
}
