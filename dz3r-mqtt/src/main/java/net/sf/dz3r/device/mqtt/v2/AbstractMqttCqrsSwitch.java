package net.sf.dz3r.device.mqtt.v2;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import net.sf.dz3r.device.actuator.CqrsSwitch;
import net.sf.dz3r.device.mqtt.v1.MqttAdapter;
import net.sf.dz3r.device.mqtt.v1.MqttMessageAddress;
import net.sf.dz3r.instrumentation.Marker;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;

import java.time.Clock;
import java.time.Duration;

public abstract class AbstractMqttCqrsSwitch extends AbstractMqttCqrsDevice<Boolean, Boolean> implements CqrsSwitch<MqttMessageAddress> {

    protected AbstractMqttCqrsSwitch(
            String id, Clock clock,
            Duration heartbeat, Duration pace,
            MqttAdapter mqttAdapter,
            String rootTopic
            ) {
        super(
                id, clock,
                heartbeat, pace,
                mqttAdapter, rootTopic
        );
    }

    @Override
    protected Boolean getCloseCommand() {
        return false;
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
    protected final Boolean translateCommand(Boolean command) {
        return command;
    }
    protected abstract String renderPayload(Boolean state);

    @Override
    public MqttMessageAddress getAddress() {
        return null;
    }
}
