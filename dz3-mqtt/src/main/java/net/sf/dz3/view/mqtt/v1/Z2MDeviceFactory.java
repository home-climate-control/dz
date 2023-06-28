package net.sf.dz3.view.mqtt.v1;

import org.eclipse.paho.client.mqttv3.MqttException;

/**
 * Generic listener that treats Z2M messages as JSON packets and allows to extract arbitrary measurements from them.
 *
 * Payloads are hardware specific (<a href="https://www.zigbee2mqtt.io/supported-devices/">read more</a>).
 *
 * For example,
 * <a href="https://sonoff.tech/product/gateway-amd-sensors/snzb-02/">SONOFF SNZB-02</a> provides the following measurements:
 *
 * <ul>
 *     <li>battery</li>
 *     <li>humidity</li>
 *     <li>linkquality</li>
 *     <li>temperature</li>
 *     <li>voltage</li>
 * </ul>
 *
 * Using {@link #getSensor(String)} will return the temperature sensor.
 *
 * Read more: <a href="https://www.zigbee2mqtt.io/guide/usage/mqtt_topics_and_messages.html">MQTT Topics and Messages</a>
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public abstract class Z2MDeviceFactory extends AbstractMqttDeviceFactory {

    /**
     * Unauthenticated constructor with a default port.
     *
     * @param mqttBrokerHost Host to connect to.
     * @param mqttRootTopicPub Root topic to publish to.
     * @param mqttRootTopicSub Root topic to subscribe to.
     */
    public Z2MDeviceFactory(
            String mqttBrokerHost,
            String mqttRootTopicPub, String mqttRootTopicSub) throws MqttException {

        super(mqttBrokerHost, MqttContext.DEFAULT_PORT, null, null, mqttRootTopicPub, mqttRootTopicSub);
    }

    /**
     * Unauthenticated constructor with a custom port.
     *
     * @param mqttBrokerHost Host to connect to.
     * @param mqttBrokerPort Port to connect to.
     * @param mqttRootTopicPub Root topic to publish to.
     * @param mqttRootTopicSub Root topic to subscribe to.
     */
    public Z2MDeviceFactory(
            String mqttBrokerHost, int mqttBrokerPort,
            String mqttRootTopicPub, String mqttRootTopicSub) throws MqttException {

        super(mqttBrokerHost, mqttBrokerPort, null, null, mqttRootTopicPub, mqttRootTopicSub);
    }

    /**
     * Authenticated constructor with a default port.
     *
     * @param mqttBrokerHost Host to connect to.
     * @param mqttBrokerUsername MQTT broker username.
     * @param mqttBrokerPassword MQTT broker password.
     * @param mqttRootTopicPub Root topic to publish to.
     * @param mqttRootTopicSub Root topic to subscribe to.
     */
    public Z2MDeviceFactory(
            String mqttBrokerHost,
            String mqttBrokerUsername, String mqttBrokerPassword,
            String mqttRootTopicPub, String mqttRootTopicSub) throws MqttException {

        super(mqttBrokerHost, MqttContext.DEFAULT_PORT, mqttBrokerUsername, mqttBrokerPassword, mqttRootTopicPub, mqttRootTopicSub);
    }

    /**
     * Authenticated constructor with a custom port.
     *
     * @param mqttBrokerHost Host to connect to.
     * @param mqttBrokerPort Port to connect to.
     * @param mqttBrokerUsername MQTT broker username.
     * @param mqttBrokerPassword MQTT broker password.
     * @param mqttRootTopicPub Root topic to publish to.
     * @param mqttRootTopicSub Root topic to subscribe to.
     */
    public Z2MDeviceFactory(
            String mqttBrokerHost, int mqttBrokerPort,
            String mqttBrokerUsername, String mqttBrokerPassword,
            String mqttRootTopicPub, String mqttRootTopicSub) throws MqttException {

        super(mqttBrokerHost, mqttBrokerPort, mqttBrokerUsername, mqttBrokerPassword, mqttRootTopicPub, mqttRootTopicSub);
    }
}
