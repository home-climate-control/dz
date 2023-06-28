package net.sf.dz3.view.mqtt.v1;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;
import net.sf.dz3.device.sensor.AnalogSensor;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@Disabled("Enable if you have a MQTT broker running on localhost (or elsewhere, see the source)")
public class SNZB02DeviceFactoryTest {

    private final Logger logger = LogManager.getLogger(getClass());

    private static final Random rg = new Random();
    private static SNZB02DeviceFactory deviceFactory;
    private static String pubTopic;
    private static String subTopic;
    @BeforeAll
    public static void init() throws MqttException {

        String root = "/dz-test-" + Math.abs(rg.nextInt()) + "/";
        pubTopic = root + "pub";
        subTopic = root + "sub";

        // VT: NOTE: Of course this will fail in Jenkins...
        deviceFactory = new SNZB02DeviceFactory("localhost", pubTopic, subTopic);
    }

    @AfterAll
    public static void shutdown() throws Exception {
        deviceFactory.powerOff();
    }

    /**
     * The MQTT message SNZB-02 is sending as of right now.
     */
    private static final String MQTT_MESSAGE_ACTUAL = "{\"battery\":100,\"humidity\":27.1,\"linkquality\":57,\"temperature\":25.7,\"voltage\":3200}";

    @Test
    void instantiate5args() throws MqttException, Exception {
        assertThatCode(() -> {
            try (Z2MDeviceFactory unused = new SNZB02DeviceFactory(
                    "localhost",
                    null, null,
                    "/dev/null", "/dev/null")) {
                // VT: NOTE: All this just to improve test coverage?
            }}).doesNotThrowAnyException();
    }

    @Test
    void instantiate6args() throws MqttException, Exception {
        assertThatCode(() -> {
            try (Z2MDeviceFactory unused = new SNZB02DeviceFactory(
                    "localhost",
                    MqttContext.DEFAULT_PORT,
                    null, null,
                    "/dev/null", "/dev/null")) {
                // VT: NOTE: All this just to improve test coverage?
            }}).doesNotThrowAnyException();
    }

    @Test
    void processPass() {
        assertThatCode(() -> {
            deviceFactory.process("zigbee2mqtt/SNZB-02-00", MQTT_MESSAGE_ACTUAL.getBytes());
        }).doesNotThrowAnyException();
    }


    @Test
    void allMandatoryPresent() {
        assertThatCode(() -> {
            assertThat(deviceFactory.checkFields("mandatory", Level.ERROR, MqttDeviceFactory.MANDATORY_JSON_FIELDS, getMandatoryFields(MQTT_MESSAGE_ACTUAL))).isTrue();
        }).doesNotThrowAnyException();
    }


    private Object[] getMandatoryFields(String message) {

        try (JsonReader reader = Json.createReader(new ByteArrayInputStream(message.getBytes()))) {
            JsonObject payload = reader.readObject();

            JsonNumber battery = payload.getJsonNumber(SNZB02DeviceFactory.Measurement.BATTERY.name);
            JsonNumber humidity = payload.getJsonNumber(SNZB02DeviceFactory.Measurement.HUMIDITY.name);
            JsonNumber linkquality = payload.getJsonNumber(SNZB02DeviceFactory.Measurement.LINKQUALITY.name);
            JsonNumber temperature = payload.getJsonNumber(SNZB02DeviceFactory.Measurement.TEMPERATURE.name);
            JsonNumber voltage = payload.getJsonNumber(SNZB02DeviceFactory.Measurement.VOLTAGE.name);

            return new Object[] {
                    battery,
                    humidity,
                    linkquality,
                    temperature,
                    voltage
            };

        }
    }

    @Test
    void getSwitch() {
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> deviceFactory.getSwitch("address"));
    }

    @Test
    void getSensor() {
        assertThat(deviceFactory.getSensor("mqtt-sensor")).isNotNull();
    }

    @Test
    @SuppressWarnings("squid:S2925")
    void getProcessSensorInput() throws InterruptedException {

        AnalogSensor s = deviceFactory.getSensor("SNZB-02-00");
        assertThat(s).isNotNull();

        var receiver = new ArrayList<DataSample<Double>>();
        SensorListener sl = new SensorListener(receiver);
        s.addConsumer(sl);

        deviceFactory.processSensorInput("SNZB-02-00", 100, 27.1, 57, 25.68, 3200);

        // Should be enough for the propagation to kick in
        // VT: NOTE: squid:S2925 it is impractical to exert extra effort for this case
        Thread.sleep(100);

        s.removeConsumer(sl);

        assertThat(receiver.get(0).sample.doubleValue()).as("receiver status").isEqualTo(25.68);
        assertThat(s.getSignal().sample.doubleValue()).as("sensor status").isEqualTo(25.68);
    }

    private static class SensorListener implements DataSink<Double> {

        private final List<DataSample<Double>> receiver;

        public SensorListener(List<DataSample<Double>> receiver) {
            this.receiver = receiver;
        }

        @Override
        public void consume(DataSample<Double> sample) {
            // The value will be limited for the purpose of the test
            receiver.add(sample);
        }
    }

    @Test
    void jmxDescriptorFactory() {
        assertThatCode(() -> {
            logDescriptor("jmxDescriptorFactory", deviceFactory.getJmxDescriptor());
        }).doesNotThrowAnyException();
    }

    @Test
    void jmxDescriptorSensor() {
        assertThatCode(() -> {
            logDescriptor("jmxDescriptorSensor", deviceFactory.getSensor("sensor").getJmxDescriptor());
        }).doesNotThrowAnyException();
    }

    private void logDescriptor(String marker, JmxDescriptor descriptor) {

        ThreadContext.push(marker);

        logger.info("description: {}", descriptor.description);
        logger.info("domainName: {}", descriptor.domainName);
        logger.info("instance: {}", descriptor.instance);
        logger.info("name: {}", descriptor.name);

        ThreadContext.pop();
    }

    @Test
    void parseTopic() {
        assertThat(deviceFactory.parseTopic("zigbee2mqtt/SNZB-02-00")).isEqualTo("SNZB-02-00");
    }
}
