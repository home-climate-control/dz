package net.sf.dz3r.device.mqtt.v1;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;

/**
 * Read/write MQTT adapter.
 *
 * Unlike {@link MqttListener} which just listens to an MQTT stream, this class supports sending MQTT messages.
 *
 */
public class MqttAdapter extends AbstractMqttAdapter {

    /**
     * Create an unauthenticated instance that will NOT automatically reconnect.
     *
     * @param address MQTT broker endpoint.
     */
    public MqttAdapter(MqttEndpoint address) {
        this(address, null, null, false, true);
    }

    public MqttAdapter(MqttEndpoint address, String username, String password, boolean autoReconnect, boolean includeSubtopics) {
        super(address, username, password, autoReconnect, includeSubtopics);
    }

    public void publish(String topic, String payload, MqttQos qos, boolean retain) {

        var message = Mqtt3Publish
                .builder()
                .topic(topic)
                .payload(payload == null ? null : payload.getBytes())
                .qos(qos)
                .retain(retain)
                .build();

        getClient().publish(message);
    }
}
