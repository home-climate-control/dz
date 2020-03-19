package net.sf.dz3.view.mqtt.v1;

import org.eclipse.paho.client.mqttv3.MqttException;

import net.sf.dz3.device.sensor.AnalogSensor;
import net.sf.dz3.device.sensor.DeviceFactory2020;
import net.sf.dz3.device.sensor.Switch;

/**
 * Factory for sensors and actuators supported via MQTT.
 *
 * The difference between this class and {@link MqttConnector} is that the
 * latter is *exposing* a set of DZ entities to outside consumers (and providing
 * a feedback mechanism), whereas this class is specifically intended to accept
 * reports from outside entities, and issue commands to them.
 *
 * VT: NOTE: this class is a radical departure from the old, complex
 * implementation of {@code OwapiDeviceFactory}, {@code XBeeDeviceFactory}, the
 * gorilla, and the whole jungle behind - the 20 year old architecture is too
 * complicated for my taste today. Let's see how much simpler it can get while
 * keeping the same design contract.
 *
 * @see MqttConnector
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2020
 */
public class MqttDeviceFactory implements DeviceFactory2020, AutoCloseable, MqttConstants {

    private final MqttContext mqtt;

    /**
     * Unauthenticated constructor with a default port.
     *
     * @param mqttBrokerHost Host to connect to.
     * @param mqttRootTopicPub Root topic to publish to.
     * @param mqttRootTopicSub Root topic to subscribe to.
     * @param initSet Entities to publish the status of.
     */
    public MqttDeviceFactory(
            String mqttBrokerHost,
            String mqttRootTopicPub, String mqttRootTopicSub) throws MqttException {

        this(mqttBrokerHost, MQTT_DEFAULT_PORT, null, null, mqttRootTopicPub, mqttRootTopicSub);
    }

    /**
     * Unauthenticated constructor with a custom port.
     *
     * @param mqttBrokerHost Host to connect to.
     * @param mqttBrokerPort Port to connect to.
     * @param mqttRootTopicPub Root topic to publish to.
     * @param mqttRootTopicSub Root topic to subscribe to.
     * @param initSet Entities to publish the status of.
     */
    public MqttDeviceFactory(
            String mqttBrokerHost, int mqttBrokerPort,
            String mqttRootTopicPub, String mqttRootTopicSub) throws MqttException {

        this(mqttBrokerHost, mqttBrokerPort, null, null, mqttRootTopicPub, mqttRootTopicSub);
    }

    /**
     * Authenticated constructor with a default port.
     *
     * @param mqttBrokerHost Host to connect to.
     * @param port Port to connect to.
     * @param mqttBrokerUsername MQTT broker username.
     * @param mqttBrokerPassword MQTT broker password.
     * @param mqttRootTopicPub Root topic to publish to.
     * @param mqttRootTopicSub Root topic to subscribe to.
     * @param initSet Entities to publish the status of.
     */
    public MqttDeviceFactory(
            String mqttBrokerHost,
            String mqttBrokerUsername, String mqttBrokerPassword,
            String mqttRootTopicPub, String mqttRootTopicSub) throws MqttException {

        this(mqttBrokerHost, MQTT_DEFAULT_PORT, mqttBrokerUsername, mqttBrokerPassword, mqttRootTopicPub, mqttRootTopicSub);
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
     * @param initSet Entities to publish the status of.
     */
    public MqttDeviceFactory(
            String mqttBrokerHost, int mqttBrokerPort,
            String mqttBrokerUsername, String mqttBrokerPassword,
            String mqttRootTopicPub, String mqttRootTopicSub) throws MqttException {

        this.mqtt = new MqttContext(
                mqttBrokerHost, mqttBrokerPort,
                mqttBrokerUsername, mqttBrokerPassword,
                mqttRootTopicPub, mqttRootTopicSub);
    }

    @Override
    public AnalogSensor getSensor(String address) {
        throw new IllegalStateException("Not Implemented");
    }

    @Override
    public Switch getSwitch(String address) {
        throw new IllegalStateException("Not Implemented");
    }

    @Override
    public void close() throws Exception {
        mqtt.client.close();
    }
}
