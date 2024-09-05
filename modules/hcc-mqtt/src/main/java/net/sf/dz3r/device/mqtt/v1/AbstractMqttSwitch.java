package net.sf.dz3r.device.mqtt.v1;

import com.homeclimatecontrol.hcc.signal.Signal;
import net.sf.dz3r.device.actuator.AbstractSwitch;
import net.sf.dz3r.device.mqtt.MqttAdapter;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;

/**
 * Common abstraction for all MQTT based switches (that'll be ESPHome, Zigbee, Z-Wave at the moment).
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public abstract class AbstractMqttSwitch extends AbstractSwitch<MqttMessageAddress> {

    protected final MqttAdapter mqttAdapter;
    private Flux<MqttSignal> mqttStateFlux;

    protected Signal<Boolean, Void> lastKnownState;

    protected AbstractMqttSwitch(
            MqttAdapter mqttAdapter,
            MqttMessageAddress address,
            Scheduler scheduler,
            Duration pace,
            boolean optimistic,
            Clock clock) {

        super(address, optimistic, scheduler, pace, clock);
        this.mqttAdapter = mqttAdapter;
    }

    protected abstract boolean includeSubtopics();
    protected abstract String getGetStateTopic();

    protected abstract String getSetStateTopic();

    protected abstract String renderPayload(boolean state);

    protected abstract Signal<Boolean, Void> parsePayload(String message);

    protected synchronized Flux<MqttSignal> getStateFlux() {

        if (mqttStateFlux != null) {
            return mqttStateFlux;
        }

        mqttStateFlux = mqttAdapter.getFlux(getGetStateTopic(), includeSubtopics());

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

            return parsePayload(mqttSignal.message());

        } finally {
            ThreadContext.pop();
        }
    }
}
