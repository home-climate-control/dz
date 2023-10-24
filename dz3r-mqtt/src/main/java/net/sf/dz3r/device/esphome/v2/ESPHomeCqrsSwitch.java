package net.sf.dz3r.device.esphome.v2;

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
 */
public class ESPHomeCqrsSwitch extends AbstractMqttCqrsSwitch {

    private String availabilityMessage;

    private final Disposable availabilityFlux;
    private final Disposable rootFlux;

    protected ESPHomeCqrsSwitch(
            String id,
            Clock clock,
            Duration heartbeat,
            Duration pace,
            MqttAdapter adapter,
            MqttMessageAddress address,
            String availabilityTopic) {
        super(id, clock, heartbeat, pace, adapter, address);

        availabilityFlux = adapter
                .getFlux(availabilityTopic, true)
                .subscribe(this::parseAvailability);
        rootFlux = adapter
                .getFlux(address.topic, true)
                .subscribe(this::parseState);
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


    private void parseAvailability(MqttSignal message) {

        this.availabilityMessage = message.message();
        stateSink.tryEmitNext(getStateSignal());
    }

    private void parseState(MqttSignal message) {
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
    protected void setStateSync(Boolean command) {

        ThreadContext.push("setStateSync");
        var m = new Marker("setStateSync", Level.TRACE);

        try {
            mqttAdapter.publish(getCommandTopic(), renderPayload(command), MqttQos.AT_LEAST_ONCE, false);
            queueDepth.decrementAndGet();
        } finally {
            m.close();
            ThreadContext.pop();
        }
    }

    @Override
    public boolean isAvailable() {
        return "online".equals(availabilityMessage);
    }

    @Override
    protected void closeSubclass2() {

        // Close the comms channel
        rootFlux.dispose();
        availabilityFlux.dispose();
    }
}
