package net.sf.dz3r.device.mqtt.v1;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;
import net.sf.dz3r.device.mqtt.MqttAdapter;
import org.apache.logging.log4j.ThreadContext;

import java.nio.charset.StandardCharsets;

/**
 * Read/write MQTT adapter.
 *
 * Unlike {@link MqttListenerImpl} which just listens to an MQTT stream, this class supports sending MQTT messages.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class MqttAdapterImpl extends MqttListenerImpl implements MqttAdapter {

    /**
     * Create an unauthenticated instance that will NOT automatically reconnect.
     *
     * @param address MQTT broker endpoint.
     */
    public MqttAdapterImpl(MqttEndpoint address) {
        this(address, null, null, false);
    }

    public MqttAdapterImpl(MqttEndpoint address, String username, String password, boolean autoReconnect) {
        super(address, username, password, autoReconnect);
    }

    public void publish(String topic, String payload, MqttQos qos, boolean retain) {

        ThreadContext.push("publish");

        try {

            logger.trace("topic={}, payload={}, qos={}, retain={}", topic, payload, qos, retain);

            var message = Mqtt3Publish
                    .builder()
                    .topic(topic)
                    .payload(payload == null ? null : payload.getBytes(StandardCharsets.UTF_8))
                    .qos(qos)
                    .retain(retain)
                    .build();

            getClient().publish(message);

        } finally {
            ThreadContext.pop();
        }
    }
}
