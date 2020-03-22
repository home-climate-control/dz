package net.sf.dz3.view.mqtt.v1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.stream.JsonParsingException;

import org.apache.logging.log4j.Level;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import net.sf.dz3.device.sensor.AnalogSensor;
import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.datastream.signal.model.DataSink;

public class MqttDeviceFactoryTest implements MqttConstants {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final static Random rg = new Random();
    private static MqttDeviceFactory mdf;
    private static String pubTopic;
    private static String subTopic;

    @BeforeClass
    public static void init() throws MqttException {

        String root = "/dz-test-" + Math.abs(rg.nextInt()) + "/";
        pubTopic = root + "pub";
        subTopic = root + "sub";

        // VT: NOTE: Of course this will fail in Jenkins...
        mdf = new MqttDeviceFactory("localhost", pubTopic, subTopic);
    }

    @AfterClass
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
    public void processPass() {
        mdf.process(MQTT_MESSAGE_ACTUAL.getBytes());
        // This is a simple pass test
        assertTrue(true);
    }

    @Test
    public void processNoEntity() {
        mdf.process(MQTT_MESSAGE_NO_ENTITY.getBytes());
        // This is a simple pass test
        assertTrue(true);
    }

    @Test
    public void processUnknown() {
        mdf.process(MQTT_MESSAGE_SWITCH.getBytes());
        // This is a simple pass test
        assertTrue(true);
    }

    @Test
    public void allMandatoryPresent() {
        assertTrue(mdf.checkFields("mandatory", Level.ERROR, MqttDeviceFactory.MANDATORY_JSON_FIELDS, getMandatoryFields(MQTT_MESSAGE_ACTUAL)));
    }

    @Test
    public void missingEntityType() {
        assertFalse(mdf.checkFields("mandatory", Level.ERROR, MqttDeviceFactory.MANDATORY_JSON_FIELDS, getMandatoryFields(MQTT_MESSAGE_NO_ENTITY)));
    }

    @Test
    public void missingName() {
        assertFalse(mdf.checkFields("mandatory", Level.ERROR, MqttDeviceFactory.MANDATORY_JSON_FIELDS, getMandatoryFields(MQTT_MESSAGE_NO_NAME)));
    }

    @Test
    public void missingSignal() {
        assertFalse(mdf.checkFields("mandatory", Level.ERROR, MqttDeviceFactory.MANDATORY_JSON_FIELDS, getMandatoryFields(MQTT_MESSAGE_NO_SIGNAL)));
    }

    @Test
    public void allOptionalPresent() {
        assertTrue(mdf.checkFields("optional", Level.WARN, MqttDeviceFactory.OPTIONAL_JSON_FIELDS, getOptionalFields(MQTT_MESSAGE_OPTIONAL)));
    }

    private Object[] getMandatoryFields(String message) {

        try (JsonReader reader = Json.createReader(new ByteArrayInputStream(message.getBytes()))) {
            JsonObject payload = reader.readObject();

            JsonString entityType = payload.getJsonString(ENTITY_TYPE);
            JsonString name = payload.getJsonString(NAME);
            JsonNumber signal = payload.getJsonNumber(SIGNAL);

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

            JsonNumber timestamp = payload.getJsonNumber(TIMESTAMP);
            JsonString signature = payload.getJsonString(SIGNATURE);
            JsonString deviceId = payload.getJsonString(DEVICE_ID);

            return new Object[] {
                    timestamp,
                signature,
                deviceId
            };

        }
    }

    @Test
    public void notJson() {

        thrown.expect(JsonParsingException.class);

        mdf.process("28C06879A20003CE: 23.5C".getBytes());
    }

    @Test
    public void getSwitch() {

        thrown.expect(UnsupportedOperationException.class);

        mdf.getSwitch("address");
    }

    @Test
    public void getSensor() {

        AnalogSensor s = mdf.getSensor("mqtt-sensor");
        assertNotNull(s);
    }

    @Test
    public void getProcessSensorInput() throws InterruptedException {

        AnalogSensor s = mdf.getSensor("mqtt-sensor");
        assertNotNull(s);

        AtomicInteger receiver = new AtomicInteger(0);
        SensorListener sl = new SensorListener(receiver);
        s.addConsumer(sl);

        mdf.processSensorInput("mqtt-sensor", new BigDecimal("42"), null, null, null);

        // Should be enough for the propagation to kick in
        Thread.sleep(100);

        s.removeConsumer(sl);

        assertEquals("wrong receiver status", 42, receiver.get());
        assertEquals("wrong sensor status", 42, s.getSignal().sample.intValue());
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
}