package net.sf.dz3r.device.mqtt.v1;

import java.util.Objects;

public class MqttMessageAddress implements Comparable<MqttMessageAddress> {

    public final MqttEndpoint endpoint;
    public final String topic;

    public MqttMessageAddress(MqttEndpoint endpoint, String topic) {
        this.endpoint = endpoint;
        this.topic = topic;
    }

    @Override
    public String toString() {
        return endpoint.toString() + "/" + topic;
    }

    @Override
    public int compareTo(MqttMessageAddress o) {
        return toString().compareTo(o.toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !getClass().isInstance(o)) {
            return false;
        }

        MqttMessageAddress that = (MqttMessageAddress) o;
        return Objects.equals(endpoint, that.endpoint) && Objects.equals(topic, that.topic);
    }

    @Override
    public int hashCode() {
        return Objects.hash(endpoint, topic);
    }
}
