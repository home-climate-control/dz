package net.sf.dz3r.device.esphome.v1;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import net.sf.dz3r.common.HCCObjects;
import net.sf.dz3r.device.actuator.VariableOutputDevice;
import net.sf.dz3r.device.mqtt.v1.MqttAdapter;
import net.sf.dz3r.device.mqtt.v1.MqttSignal;
import net.sf.dz3r.instrumentation.Marker;
import net.sf.dz3r.signal.Signal;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Driver for <a href="https://esphome.io/components/fan/index.html">ESPHome Fan Component</a>.
 *
 * Important: {@code speed_count} needs to be left at default (100) for this driver to operate correctly.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class ESPHomeFan implements VariableOutputDevice {

    private final Logger logger = LogManager.getLogger();

    private final String id;
    private final Clock clock;
    private final MqttAdapter adapter;
    private final String stateTopic;
    private final String commandTopic;
    private final String speedStateTopic;
    private final String speedCommandTopic;
    private String availabilityMessage;

    private final AtomicInteger queueDepth = new AtomicInteger();

    private final Disposable availabilityFlux;
    private final Disposable rootFlux;
    private final Sinks.Many<Command> commandSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Disposable commandSubscription;
    private final Sinks.Many<Signal<State, String>> stateSink = Sinks.many().multicast().onBackpressureBuffer();

    private OutputState requested;
    private OutputState actual;

    public ESPHomeFan(
            String id,
            MqttAdapter adapter,
            String rootTopic,
            String availabilityTopic) {

        this(
                id,
                Clock.system(ZoneId.systemDefault()),
                adapter,
                rootTopic,
                availabilityTopic
        );
    }

    public ESPHomeFan(
            String id,
            Clock clock,
            MqttAdapter adapter,
            String rootTopic,
            String availabilityTopic) {

        this.id = HCCObjects.requireNonNull(id, "id can't be null");
        this.clock = HCCObjects.requireNonNull(clock, "adapter can't be null");
        this.adapter = HCCObjects.requireNonNull(adapter, "adapter can't be null");
        HCCObjects.requireNonNull(rootTopic, "rootTopic can't be null");

        HCCObjects.requireNonNull(availabilityTopic, "availabilityTopic can't be null");

        // Defaults
        stateTopic = rootTopic + "/state";
        commandTopic = rootTopic + "/command";
        speedStateTopic = rootTopic + "/speed_level/state";
        speedCommandTopic = rootTopic + "/speed_level/command";

        commandSubscription = commandSink
                .asFlux()
                .publishOn(Schedulers.newSingle("espfan-" + id))
                .subscribe(this::setStateSync);

        availabilityFlux = adapter
                .getFlux(availabilityTopic, true)
                .subscribe(this::parseAvailability);
        rootFlux = adapter
                .getFlux(rootTopic, true)
                .subscribe(this::parseState);
    }

    private void parseAvailability(MqttSignal message) {

        this.availabilityMessage = message.message();
        stateSink.tryEmitNext(getStateSignal());
    }

    /**
     * Parse device state coming from {@link #adapter}.
     *
     * @param message Incoming MQTT message.
     */
    private void parseState(MqttSignal message) {

        // VT: NOTE: MqttAdapter has already logged the message at TRACE level

        tryParseState(message);
        tryParseSpeed(message);

        stateSink.tryEmitNext(getStateSignal());
    }

    private void tryParseState(MqttSignal message) {

        if (!stateTopic.equals(message.topic())) {
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
    public State getState() {

        return new State(
                id,
                isAvailable(),
                requested,
                actual,
                queueDepth.get()
        );
    }

    @Override
    public synchronized State setState(boolean on, double output) {

        if (output < 0 || output > 1) {
            throw new IllegalArgumentException("speed given (" + output + ") is outside of 0..1 range");
        }

        this.requested = new OutputState(on, output);
        queueDepth.incrementAndGet();
        commandSink.tryEmitNext(new Command(on, output));

        var state = getState();
        stateSink.tryEmitNext(new Signal<>(Instant.now(clock), state, id));

        return state;
    }

    /**
     * Set the requested state, synchronously
     *
     * @param command Command to execute.
     */
    private void setStateSync(Command command) {

        ThreadContext.push("setStateSync");
        var m = new Marker("setStateSync", Level.TRACE);

        try {

            // This will translate into two commands, but so be it

            adapter.publish(commandTopic, command.on() ? "ON" : "OFF", MqttQos.AT_LEAST_ONCE, false);
            adapter.publish(speedCommandTopic, Integer.toString((int) (command.output() * 100)), MqttQos.AT_LEAST_ONCE, false);

            queueDepth.decrementAndGet();

            stateSink.tryEmitNext(getStateSignal());

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
    public Flux<Signal<State, String>> getFlux() {
        return stateSink.asFlux();
    }

    private Signal<State, String> getStateSignal() {
        return new Signal<>(Instant.now(clock), getState(), id);
    }

    @Override
    public void close() throws Exception {

        // Prevent new commands from coming in
        commandSubscription.dispose();

        // Shut down the device
        setStateSync(new Command(false, 0d));

        // Adjust the queue depth - previous command skewed it
        queueDepth.incrementAndGet();

        // Emit the final notification
        stateSink.tryEmitNext(getStateSignal());

        // Indicate that we're done
        stateSink.tryEmitComplete();

        // Close the comms channel
        rootFlux.dispose();
        availabilityFlux.dispose();
        adapter.close();
    }
}
