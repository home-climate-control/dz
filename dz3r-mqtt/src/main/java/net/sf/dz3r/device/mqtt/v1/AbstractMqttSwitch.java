package net.sf.dz3r.device.mqtt.v1;

import net.sf.dz3r.device.actuator.AbstractSwitch;
import net.sf.dz3r.signal.Signal;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;

public abstract class AbstractMqttSwitch extends AbstractSwitch<MqttMessageAddress> {

    protected final MqttAdapter mqttAdapter;
    private Flux<MqttSignal> mqttStateFlux;

    protected Signal<Boolean, Void> lastKnownState;

    protected AbstractMqttSwitch(MqttMessageAddress address) {
        this(address, null, null, false, true, null, null, null);
    }

    protected AbstractMqttSwitch(MqttMessageAddress address,
                                 String username, String password,
                                 boolean reconnect,
                                 boolean includeSubtopics,
                                 Scheduler scheduler,
                                 Duration minDelay,
                                 Clock clock) {

        super(address, scheduler, minDelay, clock);
        mqttAdapter = new MqttAdapter(getAddress().endpoint, username, password, reconnect, includeSubtopics);
    }

    protected abstract String getGetStateTopic();

    protected abstract String getSetStateTopic();

    protected abstract String renderPayload(boolean state);

    protected abstract Signal<Boolean, Void> parsePayload(String message);

    protected synchronized Flux<MqttSignal> getStateFlux() {

        if (mqttStateFlux != null) {
            return mqttStateFlux;
        }

        mqttStateFlux = mqttAdapter.getFlux(getGetStateTopic());

        return mqttStateFlux;
    }
    @Override
    protected boolean getStateSync() throws IOException {
        throw new IllegalAccessError("This shouldn't have happened");
    }

    @Override
    public Mono<Boolean> getState() {

        ThreadContext.push("getStateSync");

        try {

            // lastKnownState may have been cleared by setStateSync() in subclasses
            // to force a re-read - that's fragile, but unavoidable

            if (lastKnownState != null) {
                logger.debug("returning cached state: {}", lastKnownState);
                return Mono.just(lastKnownState.getValue());
            }

            logger.debug("Awaiting new state for {}...", getGetStateTopic());

            return getStateFlux()
                    .next()
                    .map(this::getState)
                    .doOnNext(state -> logger.debug("signal @{}: {}", state.timestamp.atZone(ZoneId.systemDefault()), lastKnownState))
                    .map(Signal::getValue);

        } finally {
            ThreadContext.pop();
        }
    }

    protected Signal<Boolean, Void> getState(MqttSignal mqttSignal) {

        ThreadContext.push("getState");
        try {

            logger.debug("getState: {}", mqttSignal);

            return parsePayload(mqttSignal.message);

        } finally {
            ThreadContext.pop();
        }
    }
}
