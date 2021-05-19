package net.sf.dz3.view.mqtt.v1;

import static org.junit.Assert.assertEquals;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

public class ESPHomeDeviceFactoryTest {

    private final String deviceId = "/esphome/7AC96F";
    private final String sensorName = "htu21df-bedroom-master-temperature";
    private final String source = deviceId + "/sensor/" + sensorName + "/state";

    @Test
    public void parseTopic() {


        Pattern p = Pattern.compile("(.*)/sensor/(.*)/state");
        Matcher m = p.matcher(source);

        m.find();
        assertEquals("wrong deviceId", deviceId, m.group(1));
        assertEquals("wrong sensorName", sensorName, m.group(2));
    }

    @Test
    public void parseTopicNamed() {

        Pattern p = Pattern.compile("(?<deviceId>.*)/sensor/(?<sensorName>.*)/state");
        Matcher m = p.matcher(source);
        m.find();

        assertEquals("wrong deviceId", deviceId, m.group("deviceId"));
        assertEquals("wrong sensorName", sensorName, m.group("sensorName"));
    }
}
