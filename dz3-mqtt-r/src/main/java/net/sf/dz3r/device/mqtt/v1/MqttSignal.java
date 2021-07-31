package net.sf.dz3r.device.mqtt.v1;

import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * MQTT signal.
 *
 * Doesn't implement the {@link net.sf.dz3r.signal.Signal} interface - no need at this point,
 * DZ entities haven't been resolved yet.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class MqttSignal {

    public final String topic;
    public final MqttMessage message;

    public MqttSignal(String topic, MqttMessage message) {
        this.topic = topic;
        this.message = message;
    }
}
