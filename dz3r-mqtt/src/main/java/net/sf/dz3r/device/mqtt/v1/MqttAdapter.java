package net.sf.dz3r.device.mqtt.v1;

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
        this(address, null, null, false);
    }

    public MqttAdapter(MqttEndpoint address, String username, String password, boolean autoReconnect) {
        super(address, username, password, autoReconnect);
    }

    public void publish(String topic, String payload) {
        throw new UnsupportedOperationException("Not Implemented");
    }
}
