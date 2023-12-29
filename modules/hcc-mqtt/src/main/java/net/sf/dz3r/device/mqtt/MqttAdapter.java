package net.sf.dz3r.device.mqtt;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import net.sf.dz3r.device.mqtt.v1.MqttListenerImpl;

/**
 * Read/write MQTT adapter.
 *
 * Unlike {@link MqttListenerImpl} which just listens to an MQTT stream, this class supports sending MQTT messages.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public interface MqttAdapter extends MqttListener {

    void publish(String topic, String payload, MqttQos qos, boolean retain);
}
