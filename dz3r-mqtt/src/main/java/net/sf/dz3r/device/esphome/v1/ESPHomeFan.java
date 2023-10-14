package net.sf.dz3r.device.esphome.v1;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import net.sf.dz3r.common.HCCObjects;
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
 * This driver honors <a href="https://martinfowler.com/bliki/CQRS.html">Command Query Responsibility Segregation</a>.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class ESPHomeFan implements AutoCloseable {

    private final Logger logger = LogManager.getLogger();

    /**
     * Fan device state.
     *
     * @param id device ID.
     * @param available {@code true} if MQTT reported the device as available, {@code false} if not, or if it is unknown.
     * @param requested Requested device state.
     * @param actual Actual device state.
     * @param queueDepth Current value of {@link #queueDepth}.
     */
    public record State(
            String id,
            boolean available,
            FanState requested,
            FanState actual,
            int queueDepth
    ) {

    }

    public record FanState(
        Boolean on,
        Double speed
    ) {

    }

    private record Command(
            boolean on,
            double speed
    ) {

    }


    private final String id;
    private final Clock clock;
    private final MqttAdapter adapter;
    private final String rootTopic;
    private final String stateTopic;
    private final String commandTopic;
    private final String speedStateTopic;
    private final String speedCommandTopic;
    private final String availabilityTopic;
    private String availabilityMessage;

    private final AtomicInteger queueDepth = new AtomicInteger();

    private final Disposable availabilityFlux;
    private final Disposable rootFlux;
    private final Sinks.Many<Command> commandSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Disposable commandSubscription;
    private final Sinks.Many<Signal<State, Void>> stateSink = Sinks.many().multicast().onBackpressureBuffer();

    private FanState requested;
    private FanState actual;

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
        this.rootTopic = HCCObjects.requireNonNull(rootTopic, "rootTopic can't be null");
        this.availabilityTopic = HCCObjects.requireNonNull(availabilityTopic, "availabilityTopic can't be null");

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

    private FanState mergeState(FanState state, boolean on) {

        if (state == null) {
            return new FanState(on, null);
        }

        return new FanState(on, actual.speed);
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

    private FanState mergeSpeed(FanState state, double speed) {

        if (state == null) {
            return new FanState(null, speed);
        }

        return new FanState(state.on, speed);
    }

    /**
     * Get the instant state of the device.
     *
     * @return Actual, current state of the device, immediately.
     */
    public State getState() {

        return new State(
                id,
                isAvailable(),
                requested,
                actual,
                queueDepth.get()
        );
    }

    /**
     * Request the provided state, return immediately.
     *
     * If the device is not {@link #isAvailable() available}, the command is still accepted.
     *
     * @param on Request the device to be on, or off.
     * @param speed Request the speed, {@code 0.0} to {@code 1.0}.
     *
     * @return The result of {@link #getState()} after the command was accepted (not executed).
     */
    public synchronized State setState(boolean on, double speed) {

        if (speed < 0 || speed > 1) {
            throw new IllegalArgumentException("speed given (" + speed + ") is outside of 0..1 range");
        }

        this.requested = new FanState(on, speed);
        queueDepth.incrementAndGet();
        commandSink.tryEmitNext(new Command(on, speed));

        var state = getState();
        stateSink.tryEmitNext(new Signal<>(Instant.now(clock), state));

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

            adapter.publish(commandTopic, command.on ? "ON" : "OFF", MqttQos.AT_LEAST_ONCE, false);
            adapter.publish(speedCommandTopic, Integer.toString((int) (command.speed * 100)), MqttQos.AT_LEAST_ONCE, false);

            queueDepth.decrementAndGet();

            stateSink.tryEmitNext(getStateSignal());

        } finally {
            m.close();
            ThreadContext.pop();
        }
    }

    /**
     * Check the reported device availability.
     *
     * @return previously reported device availability, immediately.
     */
    public boolean isAvailable() {
        return "online".equals(availabilityMessage);
    }

    /**
     * Get the state change notification flux.
     *
     * Note that this sink will NOT emit error signals, however, {@link State#available} will reflect device availability
     * the moment the signal was emitted.
     *
     * @return Flux emitting signals every time the {@link #setState(boolean, double) requested} or {@link #getState() actual} state has changed.
     */
    public Flux<Signal<State, Void>> getFlux() {
        return stateSink.asFlux();
    }

    private Signal<State, Void> getStateSignal() {
        return new Signal<>(Instant.now(clock), getState());
    }

    /**
     * Close the device, synchronously.
     */
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
        adapter.close();
    }
}
