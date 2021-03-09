package net.sf.dz3.view.mqtt.v1;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import net.sf.dz3.device.sensor.AnalogSensor;
import net.sf.jukebox.datastream.signal.model.DataSample;

/**
 * The slow part of {@link MqttDeviceFactory} test. The fast part is in {@link MqttDeviceFactoryTest}.
 */
public class MqttDeviceFactoryTestSlow extends MqttDeviceFactoryTestBase {

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

    @Test
    @SuppressWarnings("squid:S2925")
    public void testStale() throws InterruptedException {

        // VT: NOTE: This test runs for about a minute - inconvenient during development.
        final String enabler = "MQTT_DEVICE_FACTORY";
        if (System.getenv(enabler) == null) {
            logger.warn("slow test skipped; set {} to any value to enable it", enabler);
            return;
        }

        long timeout = (long)(MqttDeviceFactory.STALE_AGE * 1.1);
        logger.warn("buckle up, this will take {}ms to complete", timeout);
        AnalogSensor s = mdf.getSensor("sensor");

        // VT: NOTE: squid:S2925 I get it, and know how to do it, but it's not worth the effort here
        Thread.sleep(timeout);

        DataSample<Double> signal = s.getSignal();

        assertNotNull("the error is missing", signal.error);
        assertNull("the sample should not be persent", signal.sample);
    }
}
