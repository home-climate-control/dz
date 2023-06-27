package net.sf.dz3.view.mqtt.v1;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import net.sf.dz3.device.sensor.AnalogSensor;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The slow part of {@link MqttDeviceFactory} test. The fast part is in {@link MqttDeviceFactoryTest}.
 * VT: FIXME: Get smarter about running non-unit tests - pipeline will suffer from things like this
 */
@Disabled("Enable if you have a MQTT broker running on localhost (or elsewhere, see the source)")
class MqttDeviceFactoryTestSlow extends MqttDeviceFactoryTestBase {

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

    @Test
    @SuppressWarnings("squid:S2925")
    void testStale() throws InterruptedException {

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

        assertThat(signal.error).as("signal error").isNotNull();
        assertThat(signal.sample).as("signal sample").isNull();
    }
}
