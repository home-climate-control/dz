package net.sf.dz3.view.mqtt.v1;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class ESPHomeDeviceFactoryTest {

    private final String deviceId = "/esphome/7AC96F";
    private final String sensorName = "htu21df-bedroom-master-temperature";
    private final String source = deviceId + "/sensor/" + sensorName + "/state";

    @Test
    void parseTopic() {

        Pattern p = Pattern.compile("(.*)/sensor/(.*)/state");
        Matcher m = p.matcher(source);

        m.find();

        assertThat(m.group(1)).as("deviceId").isEqualTo(deviceId);
        assertThat(m.group(2)).as("sensorName").isEqualTo(sensorName);
    }

    @Test
    void parseTopicNamed() {

        Pattern p = Pattern.compile("(?<deviceId>.*)/sensor/(?<sensorName>.*)/state");
        Matcher m = p.matcher(source);
        m.find();

        assertThat(m.group("deviceId")).as("deviceId").isEqualTo(deviceId);
        assertThat(m.group("sensorName")).as("sensorName").isEqualTo(sensorName);
    }
}
