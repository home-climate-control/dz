package net.sf.dz3.view.mqtt.v1;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import net.sf.dz3.device.sensor.AnalogSensor;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * <a href="https://sonoff.tech/product/gateway-amd-sensors/snzb-02/">SONOFF SNZB-02</a> device factory.
 *
 * This sensor provides the following measurements:
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
public class SNZB02DeviceFactory extends Z2MDeviceFactory {
    public enum Measurement {

        BATTERY("battery"),
        HUMIDITY("humidity"),
        LINKQUALITY("linkquality"),
        TEMPERATURE("temperature"),
        VOLTAGE("voltage");

        public final String name;

        private Measurement(String name) {
            this.name = name;
        }
    }

    public SNZB02DeviceFactory(String mqttBrokerHost, String mqttRootTopicPub, String mqttRootTopicSub) throws MqttException {
        super(mqttBrokerHost, mqttRootTopicPub, mqttRootTopicSub);
    }

    public SNZB02DeviceFactory(String mqttBrokerHost, int mqttBrokerPort, String mqttRootTopicPub, String mqttRootTopicSub) throws MqttException {
        super(mqttBrokerHost, mqttBrokerPort, mqttRootTopicPub, mqttRootTopicSub);
    }

    public SNZB02DeviceFactory(String mqttBrokerHost, String mqttBrokerUsername, String mqttBrokerPassword, String mqttRootTopicPub, String mqttRootTopicSub) throws MqttException {
        super(mqttBrokerHost, mqttBrokerUsername, mqttBrokerPassword, mqttRootTopicPub, mqttRootTopicSub);
    }

    public SNZB02DeviceFactory(String mqttBrokerHost, int mqttBrokerPort, String mqttBrokerUsername, String mqttBrokerPassword, String mqttRootTopicPub, String mqttRootTopicSub) throws MqttException {
        super(mqttBrokerHost, mqttBrokerPort, mqttBrokerUsername, mqttBrokerPassword, mqttRootTopicPub, mqttRootTopicSub);
    }

    /**
     * Get a sensor for a specific measurement - see the top of the class for comments.
     *
     * @param address Sensor address.
     * @param measurement Measurement to request.
     *
     * @return Virtual sensor for the requested measurement.
     */
    public AnalogSensor getSensor(String address, String measurement) {
        throw new UnsupportedOperationException("Not Implemented");
    }
    @Override
    protected MqttCallback createCallback() {
        return new Callback();
    }

    static final List<String> MEASUREMENTS = Arrays.asList(
            Measurement.BATTERY.name,
            Measurement.HUMIDITY.name,
            Measurement.LINKQUALITY.name,
            Measurement.TEMPERATURE.name,
            Measurement.VOLTAGE.name);

    void process(String name, byte[] source) {

        try (JsonReader reader = Json.createReader(new ByteArrayInputStream(source))) {

            JsonObject payload = reader.readObject();

            JsonNumber battery = payload.getJsonNumber(Measurement.BATTERY.name);
            JsonNumber humidity = payload.getJsonNumber(Measurement.HUMIDITY.name);
            JsonNumber linkquality = payload.getJsonNumber(Measurement.LINKQUALITY.name);
            JsonNumber temperature = payload.getJsonNumber(Measurement.TEMPERATURE.name);
            JsonNumber voltage = payload.getJsonNumber(Measurement.VOLTAGE.name);

            if (!checkFields(
                    "mandatory", Level.ERROR, MEASUREMENTS,
                    new Object[] {
                            battery,
                            humidity,
                            linkquality,
                            temperature,
                            voltage
                    })) {
                // The check has already complained
                return;
            }

            processSensorInput(
                    name,
                    battery.doubleValue(),
                    humidity.doubleValue(),
                    linkquality.doubleValue(),
                    temperature.doubleValue(),
                    voltage.doubleValue());
        }
    }

    /**
     * Parse the sensor name out of the topic.
     *
     * @param source Topic as a string.
     *
     * @return Sensor name.
     */
    String parseTopic(String source) {

        ThreadContext.push("parseTopic");

        try {

            // That "not a sensor" debug statement below will drive the disk into the ground, better avoid it if possible
            if (seenAlienTopic.contains(source)) {

                // Trace is rarely enabled, no big deal
                logger.trace("seen '{}' already, not matching", source);

                // VT: NOTE: squid:S1168 I'm not going to waste memory to indicate a "skip" condition
                return null;
            }

            // The typical Z2M topic will look like this:
            //
            // ${Z2M-topic-prefix}/${Z2M-sensor-name}

            var offset = source.lastIndexOf("/");

            if (offset == -1) {
                throw new IllegalArgumentException("Can't parse name out of malformed topic '" + source + "'");
            }

            var name = source.substring(offset + 1);

            logger.debug("name: {}", name);

            if (deviceMap.get(name) == null) {

                logger.debug("{}: not a sensor (this message will repeat once per run)", source);

                // We don't want to see this message again
                seenAlienTopic.add(source);

                // VT: NOTE: squid:S1168 I'm not going to waste memory to indicate a "skip" condition
                return null;
            }

            return name;

        } finally {
            ThreadContext.pop();
        }
    }


    /**
     * Check if all the values are present, and complain if they're not.
     *
     * @param reference Fields that are expected to be present.
     * @param source JSON entities parsed out of the payload.
     * @return {@code false} if some of the values are missing.
     */
    boolean checkFields(String context, Level level, List<String> reference, Object[] source) {

        var missing = new LinkedList<String>();

        for (int offset = 0; offset < reference.size(); offset++) {
            if (source[offset] == null) {
                missing.add(reference.get(offset));
            }
        }

        ThreadContext.push(context);
        missing
                .stream()
                .forEach(field -> { logger.log(level, "missing: {}", field); });
        ThreadContext.pop();

        return missing.isEmpty();
    }

    void processSensorInput(
            String name,
            double battery,
            double humidity,
            double linkquality,
            double temperature,
            double voltage) {

        ThreadContext.push("processSensorInput");
        try {

            Device<?> d = deviceMap.get(name);

            if (d == null) {
                logger.debug("not ours: {}", name);
                return;
            }

            Sensor s = (Sensor) d;
            s.inject(new DataSample<>(s.getAddress(), s.getAddress(), temperature, null));

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

                logger.debug("topic={}, payload={}", topic, message);

                String name = parseTopic(topic);

                if (name == null) {
                    return;
                }

                process(name, message.getPayload());

            } catch (Exception t) {

                // VT: NOTE: According to the docs, throwing an exception here will shut down the client - can't afford that,
                // so we'll just complain loudly

                logger.error("MQTT message caused an exception: {}", message, t);

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
