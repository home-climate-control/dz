package net.sf.dz3.view.mqtt.v1;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.stream.JsonParsingException;

import org.apache.logging.log4j.Level;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class MqttDeviceFactoryTest {

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
        mdf.close();
    }

    @Test
    public void allMandatoryPresent() {
        JsonValue[] source = {
                new JsonStringImpl(MqttConstants.ENTITY_TYPE),
                new JsonStringImpl(MqttConstants.NAME),
                new JsonStringImpl(MqttConstants.SIGNAL),
        };

        assertTrue(mdf.checkFields("mandatory", Level.ERROR, MqttDeviceFactory.MANDATORY_JSON_FIELDS, source));
    }

    @Test
    public void missingEntityType() {
        JsonValue[] source = {
                new JsonStringImpl(MqttConstants.NAME),
                new JsonStringImpl(MqttConstants.SIGNAL)
        };

        assertFalse(mdf.checkFields("mandatory", Level.ERROR, MqttDeviceFactory.MANDATORY_JSON_FIELDS, source));
    }

    @Test
    public void missingName() {
        JsonValue[] source = {
                new JsonStringImpl(MqttConstants.ENTITY_TYPE),
                new JsonStringImpl(MqttConstants.SIGNAL)
        };

        assertFalse(mdf.checkFields("mandatory", Level.ERROR, MqttDeviceFactory.MANDATORY_JSON_FIELDS, source));
    }

    @Test
    public void missingSignal() {
        JsonValue[] source = {
                new JsonStringImpl(MqttConstants.ENTITY_TYPE),
                new JsonStringImpl(MqttConstants.NAME)
        };

        assertFalse(mdf.checkFields("mandatory", Level.ERROR, MqttDeviceFactory.MANDATORY_JSON_FIELDS, source));
    }

    @Test
    public void notJson() {

        thrown.expect(JsonParsingException.class);

        mdf.process("28C06879A20003CE: 23.5C".getBytes());
    }

    @Test
    public void processPass() {
        mdf.process("{\"entityType\":\"sensor\",\"name\":\"28C06879A20003CE\",\"signature\":\"T28C06879A20003CE\",\"signal\":23.50,\"deviceId\":\"ESP8266-00621CC5\"}".getBytes());
    }

    private class JsonStringImpl implements JsonString {
        private final String value;

        public JsonStringImpl(String value) {
            this.value = value;
        }

        @Override
        public ValueType getValueType() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getString() {
            return value;
        }

        @Override
        public CharSequence getChars() {
            throw new UnsupportedOperationException();
        }
    }
}
