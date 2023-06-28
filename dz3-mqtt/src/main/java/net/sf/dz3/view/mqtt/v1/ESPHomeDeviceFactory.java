package net.sf.dz3.view.mqtt.v1;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import org.apache.logging.log4j.ThreadContext;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MQTT device factory capable of working with <a href="https://esphome.io/">ESPHome</a> devices.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class ESPHomeDeviceFactory extends AbstractMqttDeviceFactory {


    /**
     * Unauthenticated constructor with a default port.
     *
     * @param mqttBrokerHost Host to connect to.
     * @param mqttRootTopicPub Root topic to publish to.
     * @param mqttRootTopicSub Root topic to subscribe to.
     */
    public ESPHomeDeviceFactory(
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
    public ESPHomeDeviceFactory(
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
    public ESPHomeDeviceFactory(
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
    public ESPHomeDeviceFactory(
            String mqttBrokerHost, int mqttBrokerPort,
            String mqttBrokerUsername, String mqttBrokerPassword,
            String mqttRootTopicPub, String mqttRootTopicSub) throws MqttException {

        super(mqttBrokerHost, mqttBrokerPort, mqttBrokerUsername, mqttBrokerPassword, mqttRootTopicPub, mqttRootTopicSub);
    }

    @Override
    protected MqttCallback createCallback() {
        return new Callback();
    }

    void process(String topic, byte[] source) {
        ThreadContext.push("process");
        try {

            String args[] = parseTopic(topic);

            if (args == null) {
                return;
            }

            // VT: FIXME: use java.time.Clock
            processSensorInput(args[1], new BigDecimal(new String(source)), System.currentTimeMillis(), args[1], args[0]);

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * ESPHome MQTT topic matching pattern.
     *
     * It is possible to optimize it to use numbered groups, but is it worth it?
     */
    private final Pattern p = Pattern.compile("(?<deviceId>.*)/sensor/(?<sensorName>.*)/state");

    /**
     * Parse the device ID and the sensor name out of the topic.
     *
     * @param source Topic as a string.
     *
     * @return An array where the first element is the topic prefix (interpreted as device ID)
     * and the second is the sensor name.
     */
    @java.lang.SuppressWarnings({"squid:S1168"})
    private String[] parseTopic(String source) {

        // That "not a sensor" debug statement below will drive the disk into the ground, better avoid it if possible
        if (seenAlienTopic.contains(source)) {

            // Trace is rarely enabled, no big deal
            logger.trace("seen '{}' already, not matching", source);

            // VT: NOTE: squid:S1168 I'm not going to waste memory to indicate a "skip" condition
            return null;
        }

        // The typical ESPHome topic will look like this:
        //
        // ${ESPHome-topic-prefix}/sensor/${ESPHome-sensor-name}/state

        Matcher m = p.matcher(source);
        m.find();

        if (!m.matches()) {

            logger.debug("{}: not a sensor (this message will repeat once per run)", source);

            // We don't want to see this message again
            seenAlienTopic.add(source);

            // VT: NOTE: squid:S1168 I'm not going to waste memory to indicate a "skip" condition
            return null;
        }

        return new String[] { m.group("deviceId"), m.group("sensorName")};
    }

    void processSensorInput(
            String name,
            BigDecimal signal,
            long timestamp,
            String signature,
            String deviceId) {

        ThreadContext.push("processSensorInput");
        try {

            Device<?> d = deviceMap.get(name);

            if (d == null) {
                logger.debug("not ours: {}", name);
                return;
            }

            Sensor s = (Sensor) d;
            double v = signal.doubleValue();
            s.inject(new DataSample<>(timestamp, s.getAddress(), deviceId + "/" + signature, v, null));

        } finally {
            ThreadContext.pop();
        }
    }

    private class Callback implements MqttCallback {

        @Override
        public void connectionLost(Throwable cause) {
            throw new IllegalStateException("Not Implemented");
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) {
            ThreadContext.push("MQTT/messageArrived");

            try {

                // VT: NOTE: We ignore topic absolutely at this point other than for logging it
                logger.debug("{} {}", topic, message);

                // VT: NOTE: When refactoring: MqttDeviceFactory just cares about content, we
                // care about the topic as well, need to tweak the process() method signature

                process(topic, message.getPayload());

            } catch (Exception t) {

                // VT: NOTE: According to the docs, throwing an exception here will shut down the client - can't afford that,
                // so we'll just complain loudly

                ThreadContext.push("error");

                try {

                    logger.error("MQTT message caused an exception");
                    logger.error("topic: {}", topic);
                    logger.error("payload: {}", new String(message.getPayload()));
                    logger.error("trace", t);

                } finally {
                    ThreadContext.pop();
                }

            } finally {
                watchdog.release();
                ThreadContext.pop();
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            // VT: NOTE: Nothing to do here
        }
    }
}
