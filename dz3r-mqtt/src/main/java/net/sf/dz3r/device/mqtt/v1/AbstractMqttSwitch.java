package net.sf.dz3r.device.mqtt.v1;

import net.sf.dz3r.device.actuator.AbstractSwitch;
import net.sf.dz3r.signal.Signal;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;

import java.io.IOException;
import java.time.ZoneId;
import java.util.Optional;

public abstract class AbstractMqttSwitch extends AbstractSwitch<MqttMessageAddress> {

    protected final MqttAdapter mqttAdapter;
    private Flux<MqttSignal> mqttStateFlux;

    protected Signal<Boolean, Void> lastKnownState;

    protected AbstractMqttSwitch(MqttMessageAddress address) {
        this(address, null, null, false, null);
    }

    protected AbstractMqttSwitch(MqttMessageAddress address,
                                 String username, String password,
                                 boolean reconnect,
                                 Scheduler scheduler) {

        super(address, scheduler);
        mqttAdapter = new MqttAdapter(getAddress().endpoint, username, password, reconnect);
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
        return getStateSync(false);
    }

    /**
     * Read, store, and return the hardware switch value.
     *
     * @param flushCache {@code true} if the currently known value must be discarded and new value needs to be awaited.
     */
    protected boolean getStateSync(boolean flushCache) {

        ThreadContext.push("getStateSync" + (flushCache ? "+flush" : ""));

        try {

            if (flushCache) {
                lastKnownState = null;
            }

            if (lastKnownState != null) {
                logger.debug("returning cached state: {}", lastKnownState);
                return lastKnownState.getValue();
            }

            logger.debug("Awaiting new state for {}...", getGetStateTopic());

            lastKnownState = getStateFlux()
                    .map(this::getState)
                    .blockFirst();

            // Sonar is paranoid, reports blockFirst() can return null. That's a pretty bizarre corner case, but OK.

            logger.debug("signal @{}: {}",
                    Optional.ofNullable(lastKnownState)
                            .map(state -> state.timestamp.atZone(ZoneId.systemDefault()))
                            .orElse(null),
                    lastKnownState);

            return Optional.ofNullable(lastKnownState)
                    .map(Signal::getValue)
                    .orElseThrow(() -> new IllegalStateException("null lastKnownState - something is seriously wrong"));

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
