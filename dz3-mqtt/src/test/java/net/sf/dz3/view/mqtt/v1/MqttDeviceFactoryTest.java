package net.sf.dz3.view.mqtt.v1;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;
import net.sf.dz3.device.sensor.AnalogSensor;
import net.sf.dz3r.device.mqtt.v1.MqttEndpoint;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.stream.JsonParsingException;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @see MqttDeviceFactoryTestSlow
 *
 * VT: FIXME: Get smarter about running non-unit tests - pipeline will suffer from things like this
 */
@Disabled("Enable if you have a MQTT broker running on localhost (or elsewhere, see the source)")
public class MqttDeviceFactoryTest extends MqttDeviceFactoryTestBase {

    @BeforeAll
    public static void init() throws MqttException {

        String root = "/dz-test-" + Math.abs(rg.nextInt()) + "/";
        pubTopic = root + "pub";
        subTopic = root + "sub";

        // VT: NOTE: Of course this will fail in Jenkins...
        mdf = new MqttDeviceFactory("localhost", pubTopic, subTopic);
    }

    @AfterAll
    public static void shutdown() throws Exception {
        mdf.powerOff();
    }

    /**
     * The MQTT message hcc-ESP8266 is sending as of right now.
     */
    private static final String MQTT_MESSAGE_ACTUAL = "{\"entity_type\":\"sensor\",\"name\":\"28C06879A20003CE\",\"signature\":\"T28C06879A20003CE\",\"signal\":23.50,\"device_id\":\"ESP8266-00621CC5\"}";

    private static final String MQTT_MESSAGE_NO_ENTITY = "{\"name\":\"28C06879A20003CE\",\"signature\":\"T28C06879A20003CE\",\"signal\":23.50,\"device_id\":\"ESP8266-00621CC5\"}";
    private static final String MQTT_MESSAGE_NO_NAME = "{\"entity_type\":\"sensor\",\"signature\":\"T28C06879A20003CE\",\"signal\":23.50,\"device_id\":\"ESP8266-00621CC5\"}";
    private static final String MQTT_MESSAGE_NO_SIGNAL = "{\"entity_type\":\"sensor\",\"name\":\"28C06879A20003CE\",\"signature\":\"T28C06879A20003CE\",\"device_id\":\"ESP8266-00621CC5\"}";
    private static final String MQTT_MESSAGE_OPTIONAL = "{\"timestamp\":1584829660855,\"signature\":\"T28C06879A20003CE\",\"device_id\":\"ESP8266-00621CC5\"}";
    private static final String MQTT_MESSAGE_SWITCH = "{\"entity_type\":\"switch\",\"name\":\"switch\",\"signature\":\"Sswitch\",\"signal\":23.50,\"device_id\":\"ESP8266-00621CC5\"}";

    @Test
    public void instantiate5args() throws MqttException, Exception {
        try (MqttDeviceFactory unused = new MqttDeviceFactory(
                "localhost",
                null, null,
                "/dev/null", "/dev/null")) {
            // VT: NOTE: All this just to improve test coverage?
        }
    }

    @Test
    public void instantiate6args() throws MqttException, Exception {
        try (MqttDeviceFactory unused = new MqttDeviceFactory(
                "localhost",
                MqttEndpoint.DEFAULT_PORT,
                null, null,
                "/dev/null", "/dev/null")) {
            // VT: NOTE: All this just to improve test coverage?
        }
    }

    @Test
    public void processPass() {
        mdf.process(MQTT_MESSAGE_ACTUAL.getBytes());
        // This is a simple pass test
        assertThat(true).isTrue();
    }

    @Test
    public void processNoEntity() {
        mdf.process(MQTT_MESSAGE_NO_ENTITY.getBytes());
        // This is a simple pass test
        assertThat(true).isTrue();
    }

    @Test
    public void processUnknown() {
        mdf.process(MQTT_MESSAGE_SWITCH.getBytes());
        // This is a simple pass test
        assertThat(true).isTrue();
    }

    @Test
    public void allMandatoryPresent() {
        assertThat(mdf.checkFields("mandatory", Level.ERROR, MqttDeviceFactory.MANDATORY_JSON_FIELDS, getMandatoryFields(MQTT_MESSAGE_ACTUAL))).isTrue();
    }

    @Test
    public void missingEntityType() {
        assertThat(mdf.checkFields("mandatory", Level.ERROR, MqttDeviceFactory.MANDATORY_JSON_FIELDS, getMandatoryFields(MQTT_MESSAGE_NO_ENTITY))).isFalse();
    }

    @Test
    public void missingName() {
        assertThat(mdf.checkFields("mandatory", Level.ERROR, MqttDeviceFactory.MANDATORY_JSON_FIELDS, getMandatoryFields(MQTT_MESSAGE_NO_NAME))).isFalse();
    }

    @Test
    public void missingSignal() {
        assertThat(mdf.checkFields("mandatory", Level.ERROR, MqttDeviceFactory.MANDATORY_JSON_FIELDS, getMandatoryFields(MQTT_MESSAGE_NO_SIGNAL))).isFalse();
    }

    @Test
    public void allOptionalPresent() {
        assertThat(mdf.checkFields("optional", Level.WARN, MqttDeviceFactory.OPTIONAL_JSON_FIELDS, getOptionalFields(MQTT_MESSAGE_OPTIONAL))).isTrue();
    }

    private Object[] getMandatoryFields(String message) {

        try (JsonReader reader = Json.createReader(new ByteArrayInputStream(message.getBytes()))) {
            JsonObject payload = reader.readObject();

            JsonString entityType = payload.getJsonString(MqttContext.JsonTag.ENTITY_TYPE.name);
            JsonString name = payload.getJsonString(MqttContext.JsonTag.NAME.name);
            JsonNumber signal = payload.getJsonNumber(MqttContext.JsonTag.SIGNAL.name);

            return new Object[] {
                entityType,
                name,
                signal
            };

        }
    }

    private Object[] getOptionalFields(String message) {

        try (JsonReader reader = Json.createReader(new ByteArrayInputStream(message.getBytes()))) {
            JsonObject payload = reader.readObject();

            JsonNumber timestamp = payload.getJsonNumber(MqttContext.JsonTag.TIMESTAMP.name);
            JsonString signature = payload.getJsonString(MqttContext.JsonTag.SIGNATURE.name);
            JsonString deviceId = payload.getJsonString(MqttContext.JsonTag.DEVICE_ID.name);

            return new Object[] {
                    timestamp,
                signature,
                deviceId
            };

        }
    }

    @Test
    public void notJson() {

        assertThatExceptionOfType(JsonParsingException.class)
                .isThrownBy(() -> mdf.process("28C06879A20003CE: 23.5C".getBytes()));
    }

    @Test
    public void getSwitch() {

        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> mdf.getSwitch("address"));
    }

    @Test
    public void getSensor() {

        assertThat(mdf.getSensor("mqtt-sensor")).isNotNull();
    }

    @Test
    @java.lang.SuppressWarnings("squid:S2925")
    public void getProcessSensorInput() throws InterruptedException {

        AnalogSensor s = mdf.getSensor("mqtt-sensor");
        assertThat(s).isNotNull();

        AtomicInteger receiver = new AtomicInteger(0);
        SensorListener sl = new SensorListener(receiver);
        s.addConsumer(sl);

        mdf.processSensorInput("mqtt-sensor", new BigDecimal("42"), null, null, null);

        // Should be enough for the propagation to kick in
        // VT: NOTE: squid:S2925 it is impractical to exert extra effort for this case
        Thread.sleep(100);

        s.removeConsumer(sl);

        assertThat(receiver.get()).as("receiver status").isEqualTo(42);
        assertThat(s.getSignal().sample.intValue()).as("sensor status").isEqualTo(42);
    }

    private class SensorListener implements DataSink<Double> {

        private final AtomicInteger receiver;

        public SensorListener(AtomicInteger receiver) {
            this.receiver = receiver;
        }

        @Override
        public void consume(DataSample<Double> sample) {
            // The value will be limited for the purpose of the test
            receiver.set(sample.sample.intValue());
        }
    }

    @Test
    public void jmxDescriptorFactory() {
        logDescriptor("jmxDescriptorFactory", mdf.getJmxDescriptor());
    }

    @Test
    public void jmxDescriptorSensor() {
        logDescriptor("jmxDescriptorSensor", mdf.getSensor("sensor").getJmxDescriptor());
    }

    private void logDescriptor(String marker, JmxDescriptor descriptor) {

        ThreadContext.push(marker);


        logger.info("description: {}", descriptor.description);
        logger.info("domainName: {}", descriptor.domainName);
        logger.info("instance: {}", descriptor.instance);
        logger.info("name: {}", descriptor.name);

        ThreadContext.pop();
    }
}
