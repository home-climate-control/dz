package net.sf.dz3r.device.esphome.v2;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import net.sf.dz3r.common.HCCObjects;
import net.sf.dz3r.device.actuator.VariableOutputDevice;
import net.sf.dz3r.device.mqtt.MqttAdapter;
import net.sf.dz3r.device.mqtt.v1.MqttSignal;
import net.sf.dz3r.device.mqtt.v2.AbstractMqttCqrsDevice;
import net.sf.dz3r.instrumentation.Marker;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;

import java.time.Clock;
import java.time.Duration;

import static net.sf.dz3r.device.actuator.VariableOutputDevice.Command;
import static net.sf.dz3r.device.actuator.VariableOutputDevice.OutputState;

/**
 * Driver for <a href="https://esphome.io/components/fan">ESPHome Fan Component</a>.
 *
 * Important: {@code speed_count} needs to be left at default (100) for this driver to operate correctly.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class ESPHomeFan extends AbstractMqttCqrsDevice<Command, OutputState> implements VariableOutputDevice {

    private final String speedStateTopic;
    private final String speedCommandTopic;
    private final String availabilityTopic;

    public ESPHomeFan(
            String id,
            Clock clock,
            Duration heartbeat,
            Duration pace,
            MqttAdapter mqttAdapter,
            String rootTopic,
            String availabilityTopic) {
        super(
                id, clock,
                heartbeat, pace,
                mqttAdapter, rootTopic);

        this.availabilityTopic = HCCObjects.requireNonNull(availabilityTopic, "esphome.fans.availability-topic can't be null (id=" + id + ")");

        // Defaults
        speedStateTopic = rootTopic + "/speed_level/state";
        speedCommandTopic = rootTopic + "/speed_level/command";
    }

    @Override
    protected boolean includeSubtopics() {
        return true;
    }

    @Override
    protected String getAvailabilityTopic() {
        return availabilityTopic;
    }

    /**
     * Parse device state coming from the {@link #mqttAdapter}.
     *
     * @param message Incoming MQTT message.
     */
    @Override
    protected void parseState(MqttSignal message) {

        // VT: NOTE: MqttAdapterImpl has already logged the message at TRACE level

        tryParseState(message);
        tryParseSpeed(message);

        stateSink.tryEmitNext(getStateSignal());
    }

    @Override
    protected String getStateTopic() {
        return rootTopic + "/state";
    }

    @Override
    protected String getCommandTopic() {
        return rootTopic + "/command";
    }

    private void tryParseState(MqttSignal message) {

        if (!getStateTopic().equals(message.topic())) {
            return;
        }

        switch (message.message()) {
            case "OFF" -> actual = mergeState(actual, false);
            case "ON" -> actual = mergeState(actual, true);
            default -> logger.error("{}: can't parse state from {}", id, message);
        }
    }

    private OutputState mergeState(OutputState state, boolean on) {

        if (state == null) {
            return new OutputState(on, null);
        }

        return new OutputState(on, actual.output());
    }

    private void tryParseSpeed(MqttSignal message) {

        if (!speedStateTopic.equals(message.topic())) {
            return;
        }

        try {

            var speed = Integer.parseInt(message.message());

            actual = mergeSpeed(actual, speed / 100d);

        } catch (NumberFormatException ex) {
            logger.error("{}: can't parse speed from {}", id, message, ex);
        }
    }

    private OutputState mergeSpeed(OutputState state, double speed) {

        if (state == null) {
            return new OutputState(null, speed);
        }

        return new OutputState(state.on(), speed);
    }

    @Override
    protected void checkCommand(Command command) {

        super.checkCommand(command);

        if (command.output() < 0 || command.output() > 1) {
            throw new IllegalArgumentException("speed given (" + command.output() + ") is outside of 0..1 range");
        }
    }

    @Override
    protected OutputState translateCommand(Command command) {
        return new OutputState(command.on(), command.output());
    }

    /**
     * Set the requested state, synchronously
     *
     * @param command Command to execute.
     */
    @Override
    protected void setStateSync(Command command) {

        ThreadContext.push("setStateSync");
        var m = new Marker("setStateSync", Level.TRACE);

        try {

            // This will translate into two commands, but so be it

            mqttAdapter.publish(getCommandTopic(), command.on() ? "ON" : "OFF", MqttQos.AT_LEAST_ONCE, false);
            mqttAdapter.publish(speedCommandTopic, Integer.toString((int) (command.output() * 100)), MqttQos.AT_LEAST_ONCE, false);

            queueDepth.decrementAndGet();

            stateSink.tryEmitNext(getStateSignal());

        } finally {
            m.close();
            ThreadContext.pop();
        }
    }

    @Override
    protected Command getCloseCommand() {
        return new Command(false, 0d);
    }

    @Override
    public boolean isAvailable() {
        return "online".equals(availabilityMessage);
    }
}
