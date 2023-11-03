package net.sf.dz3r.device.mqtt.v2;

import net.sf.dz3r.common.HCCObjects;
import net.sf.dz3r.device.DeviceState;
import net.sf.dz3r.device.actuator.AbstractCqrsDevice;
import net.sf.dz3r.device.mqtt.v1.MqttAdapter;
import net.sf.dz3r.device.mqtt.v1.MqttSignal;
import net.sf.dz3r.signal.Signal;
import reactor.core.Disposable;

import java.time.Clock;
import java.time.Duration;

public abstract class AbstractMqttCqrsDevice<I, O> extends AbstractCqrsDevice<I, O> {

    protected final MqttAdapter mqttAdapter;
    protected final String rootTopic;

    protected String availabilityMessage;

    private Disposable availabilityFlux;
    private final Disposable rootFlux;

    protected AbstractMqttCqrsDevice(
            String id, Clock clock,
            Duration heartbeat, Duration pace,
            MqttAdapter mqttAdapter,
            String rootTopic
    ) {
        super(id, clock, heartbeat, pace);

        this.mqttAdapter = HCCObjects.requireNonNull(mqttAdapter, "mqttAdapter can't be null");
        this.rootTopic = HCCObjects.requireNonNull(rootTopic, "rootTopic can't be null");

        rootFlux = initRootFlux();
    }

    protected Disposable initAvailabilityFlux() {
        return mqttAdapter
                .getFlux(getAvailabilityTopic(), false)
                .subscribe(this::parseAvailability);
    }

    protected abstract boolean includeSubtopics();
    protected abstract String getAvailabilityTopic();
    protected abstract String getStateTopic();
    protected abstract String getCommandTopic();

    protected void parseAvailability(MqttSignal message) {
        logger.trace("{}: availability={}", id, message);
        this.availabilityMessage = message.message();
        stateSink.tryEmitNext(getStateSignal());
    }

    protected Disposable initRootFlux() {
        return mqttAdapter
                .getFlux(rootTopic, includeSubtopics())
                .subscribe(this::parseState);
    }

    protected abstract void parseState(MqttSignal mqttSignal);

    @Override
    public final DeviceState<O> setState(I newState) {

        // VT: NOTE: Ugly hack to allow ESPHome classes to use availability topic not available to the constructor of this class
        synchronized (this) {

            if (availabilityFlux == null) {
                availabilityFlux = initAvailabilityFlux();
            }
        }

        checkCommand(newState);

        this.requested = translateCommand(newState);
        queueDepth.incrementAndGet();
        commandSink.tryEmitNext(newState);

        var state = getState();
        stateSink.tryEmitNext(new Signal<>(clock.instant(), state, id));

        return state;
    }

    /**
     * Translate the command into state.
     *
     * @param command Command to interpret.
     * @return State it translates into.
     */
    protected abstract O translateCommand(I command);

    /**
     * Make sure the input argument is sane.
     *
     * @param command Command to check.
     */
    protected void checkCommand(I command) {
        HCCObjects.requireNonNull(command, "command can't be null");
    }

    @Override
    protected final void closeSubclass() throws Exception {

        // Close the comms channel
        rootFlux.dispose();
        availabilityFlux.dispose();

        mqttAdapter.close();
    }
}
