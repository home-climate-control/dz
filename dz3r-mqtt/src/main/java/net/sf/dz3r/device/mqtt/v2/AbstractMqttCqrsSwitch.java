package net.sf.dz3r.device.mqtt.v2;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import net.sf.dz3r.common.HCCObjects;
import net.sf.dz3r.device.DeviceState;
import net.sf.dz3r.device.actuator.AbstractCqrsDevice;
import net.sf.dz3r.device.actuator.CqrsSwitch;
import net.sf.dz3r.device.mqtt.v1.MqttAdapter;
import net.sf.dz3r.device.mqtt.v1.MqttMessageAddress;
import net.sf.dz3r.device.mqtt.v1.MqttSignal;
import net.sf.dz3r.instrumentation.Marker;
import net.sf.dz3r.signal.Signal;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.Disposable;

import java.time.Clock;
import java.time.Duration;

public abstract class AbstractMqttCqrsSwitch extends AbstractCqrsDevice<Boolean, Boolean> implements CqrsSwitch<MqttMessageAddress> {

    protected final MqttAdapter mqttAdapter;
    protected final MqttMessageAddress address;

    protected String availabilityMessage;

    private Disposable availabilityFlux;
    private final Disposable rootFlux;

    protected AbstractMqttCqrsSwitch(
            String id, Clock clock,
            Duration heartbeat, Duration pace,
            MqttAdapter mqttAdapter,
            MqttMessageAddress address
            ) {
        super(id, clock, heartbeat, pace);

        this.mqttAdapter = HCCObjects.requireNonNull(mqttAdapter, "mqttAdapter can't be null");
        this.address = HCCObjects.requireNonNull(address, "address can't be null");

        rootFlux = initRootFlux();
    }

    protected Disposable initAvailabilityFlux() {
        return mqttAdapter
                .getFlux(getAvailabilityTopic(), true)
                .subscribe(this::parseAvailability);
    }

    protected abstract String getAvailabilityTopic();

    protected void parseAvailability(MqttSignal message) {
        this.availabilityMessage = message.message();
        stateSink.tryEmitNext(getStateSignal());
    }

    protected Disposable initRootFlux() {
        return mqttAdapter
                .getFlux(address.topic, true)
                .subscribe(this::parseState);
    }

    protected abstract void parseState(MqttSignal mqttSignal);

    protected abstract String getStateTopic();
    protected abstract String getCommandTopic();
    protected abstract String renderPayload(boolean state);

    @Override
    protected Boolean getCloseCommand() {
        return false;
    }

    @Override
    protected final void closeSubclass() throws Exception {

        // Close the comms channel
        rootFlux.dispose();
        availabilityFlux.dispose();

        mqttAdapter.close();
    }

    @Override
    public DeviceState<Boolean> setState(Boolean newState) {

        // VT: NOTE: Ugly hack to allow ESPHome classes to use availability topic not available to the constructor of this class
        synchronized (this) {

            if (availabilityFlux == null) {
                availabilityFlux = initAvailabilityFlux();
            }
        }

        this.requested = newState;
        queueDepth.incrementAndGet();
        commandSink.tryEmitNext(newState);

        var state = getState();
        stateSink.tryEmitNext(new Signal<>(clock.instant(), state, id));

        return state;
    }

    @Override
    protected final void setStateSync(Boolean command) {

        ThreadContext.push("setStateSync");
        var m = new Marker("setStateSync", Level.TRACE);

        try {
            mqttAdapter.publish(
                    getCommandTopic(),
                    renderPayload(command),
                    MqttQos.AT_LEAST_ONCE,
                    false);
            queueDepth.decrementAndGet();

        } finally {
            m.close();
            ThreadContext.pop();
        }
    }

    @Override
    public MqttMessageAddress getAddress() {
        return null;
    }
}
