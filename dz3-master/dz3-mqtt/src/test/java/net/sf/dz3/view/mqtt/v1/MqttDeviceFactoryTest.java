package net.sf.dz3.view.mqtt.v1;

import java.util.Random;

import javax.json.stream.JsonParsingException;

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
    public void notJson() {

        thrown.expect(JsonParsingException.class);
        //thrown.expectMessage("null queue, doesn't make sense");

        mdf.process("28C06879A20003CE: 23.5C".getBytes());
    }
}
