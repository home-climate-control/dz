package net.sf.dz3r.device.mqtt.v2;

import net.sf.dz3r.common.HCCObjects;
import net.sf.dz3r.device.DeviceState;
import net.sf.dz3r.device.actuator.AbstractCqrsDevice;
import net.sf.dz3r.device.actuator.CqrsSwitch;
import net.sf.dz3r.device.mqtt.v1.MqttAdapter;
import net.sf.dz3r.device.mqtt.v1.MqttMessageAddress;
import net.sf.dz3r.signal.Signal;

import java.time.Clock;
import java.time.Duration;

public abstract class AbstractMqttCqrsSwitch extends AbstractCqrsDevice<Boolean, Boolean> implements CqrsSwitch<MqttMessageAddress> {

    protected final MqttAdapter mqttAdapter;
    protected final MqttMessageAddress address;

    protected AbstractMqttCqrsSwitch(
            String id, Clock clock,
            Duration heartbeat, Duration pace,
            MqttAdapter mqttAdapter,
            MqttMessageAddress address
            ) {
        super(id, clock, heartbeat, pace);

        this.mqttAdapter = HCCObjects.requireNonNull(mqttAdapter, "mqttAdapter can't be null");
        this.address = HCCObjects.requireNonNull(address, "address can't be null");
    }

    protected abstract String getStateTopic();
    protected abstract String getCommandTopic();
    protected abstract String renderPayload(boolean state);

    @Override
    protected Boolean getCloseCommand() {
        return false;
    }

    @Override
    protected final void closeSubclass() throws Exception {
        closeSubclass2();
        mqttAdapter.close();
    }

    protected abstract void closeSubclass2();

    @Override
    public DeviceState<Boolean> setState(Boolean newState) {

        this.requested = newState;
        queueDepth.incrementAndGet();
        commandSink.tryEmitNext(newState);

        var state = getState();
        stateSink.tryEmitNext(new Signal<>(clock.instant(), state, id));

        return state;
    }

    @Override
    public MqttMessageAddress getAddress() {
        return null;
    }
}
