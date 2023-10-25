package net.sf.dz3r.device.esphome.v2;

import net.sf.dz3r.device.mqtt.v1.MqttAdapter;
import net.sf.dz3r.device.mqtt.v1.MqttMessageAddress;
import net.sf.dz3r.device.mqtt.v1.MqttSignal;
import net.sf.dz3r.device.mqtt.v2.AbstractMqttCqrsSwitch;

import java.time.Clock;
import java.time.Duration;

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
    protected ESPHomeCqrsSwitch(
            String id,
            Clock clock,
            Duration heartbeat,
            Duration pace,
            MqttAdapter adapter,
            MqttMessageAddress address,
            String availabilityTopic) {
        super(id, clock, heartbeat, pace, adapter, address);

        this.availabilityTopic = availabilityTopic;
    }

    @Override
    protected String getAvailabilityTopic() {
        return availabilityTopic;
    }

    @Override
    protected String getStateTopic() {
        return address.topic + "/state";
    }

    @Override
    protected String getCommandTopic() {
        return address.topic + "/command";
    }

    @Override
    protected String renderPayload(boolean state) {
        return state ? "ON" : "OFF";
    }


    @Override
    protected void parseAvailability(MqttSignal message) {

        this.availabilityMessage = message.message();
        stateSink.tryEmitNext(getStateSignal());
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
        return "online".equals(availabilityMessage);
    }
}
