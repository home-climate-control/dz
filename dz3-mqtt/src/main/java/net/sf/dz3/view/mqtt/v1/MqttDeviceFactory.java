package net.sf.dz3.view.mqtt.v1;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
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
import javax.json.JsonString;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Factory for sensors and actuators supported via MQTT.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class MqttDeviceFactory extends AbstractMqttDeviceFactory {

    /**
     * Unauthenticated constructor with a default port.
     *
     * @param mqttBrokerHost Host to connect to.
     * @param mqttRootTopicPub Root topic to publish to.
     * @param mqttRootTopicSub Root topic to subscribe to.
     */
    public MqttDeviceFactory(
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
    public MqttDeviceFactory(
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
    public MqttDeviceFactory(
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
    public MqttDeviceFactory(
            String mqttBrokerHost, int mqttBrokerPort,
            String mqttBrokerUsername, String mqttBrokerPassword,
            String mqttRootTopicPub, String mqttRootTopicSub) throws MqttException {

        super(mqttBrokerHost, mqttBrokerPort, mqttBrokerUsername, mqttBrokerPassword, mqttRootTopicPub, mqttRootTopicSub);
    }

    @Override
    protected MqttCallback createCallback() {
        return new Callback();
    }

    static final List<String> MANDATORY_JSON_FIELDS = Arrays.asList(
            MqttContext.JsonTag.ENTITY_TYPE.name,
            MqttContext.JsonTag.NAME.name,
            MqttContext.JsonTag.SIGNAL.name);
    static final List<String> OPTIONAL_JSON_FIELDS = Arrays.asList(
            MqttContext.JsonTag.TIMESTAMP.name,
            MqttContext.JsonTag.SIGNATURE.name,
            MqttContext.JsonTag.DEVICE_ID.name);

    void process(byte[] source) {
        try (JsonReader reader = Json.createReader(new ByteArrayInputStream(source))) {

            JsonObject payload = reader.readObject();

            JsonString entityType = payload.getJsonString(MqttContext.JsonTag.ENTITY_TYPE.name);
            JsonString name = payload.getJsonString(MqttContext.JsonTag.NAME.name);
            JsonNumber signal = payload.getJsonNumber(MqttContext.JsonTag.SIGNAL.name);

            JsonNumber timestamp = payload.getJsonNumber(MqttContext.JsonTag.TIMESTAMP.name);
            JsonString signature = payload.getJsonString(MqttContext.JsonTag.SIGNATURE.name);
            JsonString deviceId = payload.getJsonString(MqttContext.JsonTag.DEVICE_ID.name);

            // We're not using these yet, but let's make sure they're present
            // That'll help figuring out whether we need them in the future.
            checkFields(
                    "optional", Level.DEBUG, OPTIONAL_JSON_FIELDS,
                    new Object[] {
                            timestamp,
                            signature,
                            deviceId
                            });

            if (!checkFields(
                    "mandatory", Level.ERROR, MANDATORY_JSON_FIELDS,
                    new Object[] {
                            entityType,
                            name,
                            signal
                            })) {
                // The check has already complained
                return;
            }

            switch (entityType.getString()) {

            case "sensor":

                processSensorInput(name.getString(), signal.bigDecimalValue(), timestamp, signature, deviceId);
                return;

            default:
                logger.warn("can't process {} yet", entityType.getString());
            }
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

        List<String> missing = new LinkedList<>();
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
            BigDecimal signal,
            JsonNumber timestamp,
            JsonString signature,
            JsonString deviceId) {
        ThreadContext.push("processSensorInput");
        try {

            Device<?> d = deviceMap.get(name);

            if (d == null) {
                logger.debug("not ours: {}", name);
                return;
            }

            Sensor s = (Sensor) d;
            double v = signal.doubleValue();
            s.inject(new DataSample<>(s.getAddress(), s.getAddress(), v, null));

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

                process(message.getPayload());

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
