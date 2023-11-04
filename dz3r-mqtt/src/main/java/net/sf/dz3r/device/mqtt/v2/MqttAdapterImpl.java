package net.sf.dz3r.device.mqtt.v2;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import net.sf.dz3r.device.mqtt.MqttAdapter;
import net.sf.dz3r.device.mqtt.v1.MqttEndpoint;
import net.sf.dz3r.instrumentation.Marker;
import org.apache.logging.log4j.ThreadContext;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.apache.logging.log4j.Level.DEBUG;

/**
 * Read/write MQTT v5 adapter, v2.
 *
 * Unlike {@link MqttListenerImpl} which just listens to an MQTT stream, this class supports sending MQTT messages.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class MqttAdapterImpl extends MqttListenerImpl implements MqttAdapter {

    private Mqtt5BlockingClient blockingClient;
    public MqttAdapterImpl(MqttEndpoint address) {
        super(address);
    }

    public MqttAdapterImpl(MqttEndpoint address, String username, String password, boolean autoReconnect) {
        super(address, username, password, autoReconnect);
    }

    public MqttAdapterImpl(MqttEndpoint address, String username, String password, boolean autoReconnect, Duration cacheFor) {
        super(address, username, password, autoReconnect, cacheFor);
    }

    private synchronized Mqtt5BlockingClient getBlockingClient() {

        if (blockingClient == null) {

            // This operation is at least memory expensive, so let's just do it once
            blockingClient = getClient().toBlocking();
        }

        return blockingClient;
    }

    @Override
    public void publish(String topic, String payload, MqttQos qos, boolean retain) {

        ThreadContext.push("publish");
        Marker m = new Marker("publish", DEBUG);

        try {

            var message = Mqtt5Publish
                    .builder()
                    .topic(topic)
                    .payload(payload == null ? null : payload.getBytes(StandardCharsets.UTF_8))
                    .qos(qos)
                    .retain(retain)
                    .build();

            logger.trace("{}", message);

            getBlockingClient().publish(message);

        } finally {
            m.close();
            ThreadContext.pop();
        }
    }
}
