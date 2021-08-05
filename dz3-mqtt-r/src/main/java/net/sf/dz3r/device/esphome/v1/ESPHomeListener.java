package net.sf.dz3r.device.esphome.v1;

import net.sf.dz3r.device.Addressable;
import net.sf.dz3r.device.mqtt.v1.MqttEndpoint;
import net.sf.dz3r.device.mqtt.v1.MqttListener;
import net.sf.dz3r.device.mqtt.v1.MqttSignal;
import net.sf.dz3r.signal.Signal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ESPHomeListener implements Addressable<MqttEndpoint> {

    protected final Logger logger = LogManager.getLogger();
    private final Set<String> seenAlienTopic = new HashSet<>();

    private final MqttListener mqttListener;
    private final String mqttRootTopicSub;

    public ESPHomeListener(String host, String mqttRootTopicSub) {
        this(host, MqttEndpoint.DEFAULT_PORT, null, null, false, mqttRootTopicSub);
    }

    public ESPHomeListener(String host, int port,
                           String username, String password,
                           boolean reconnect,
                           String mqttRootTopicSub) {

        mqttListener = new MqttListener(new MqttEndpoint(host, port), username, password, reconnect);
        this.mqttRootTopicSub = mqttRootTopicSub;
    }

    @Override
    public MqttEndpoint getAddress() {
        return mqttListener.address;
    }

    public Flux<Signal<Double>> getSensorFlux(String address) {
        return mqttListener
                .getFlux(mqttRootTopicSub)
                .filter(e -> matchSensorAddress(e, address))
                .map(this::mqtt2sensor);

    }

    private boolean matchSensorAddress(MqttSignal signal, String address) {
        return signal.topic.endsWith("sensor/" + address + "/state");
    }

    private Signal<Double> mqtt2sensor(MqttSignal mqttSignal) {

        // This is ESPHome, they don't provide timestamps.
        // It is possible to get a stale value if LWT wasn't set up correctly

        var timestamp = Instant.now(); // NOSONAR false positive

        return new Signal<>(
                timestamp,
                Double.parseDouble(mqttSignal.message));
    }

    private String getAddress(String topic) {
        return parseTopic(topic)[1];
    }

    /**
     * ESPHome MQTT topic matching pattern.
     *
     * It is possible to optimize it to use numbered groups, but is it worth it?
     */
    private final Pattern p = Pattern.compile("(?<deviceId>.*)/sensor/(?<sensorName>.*)/state");

    /**
     * Parse the device ID and the sensor name out of the topic.
     *
     * @param source Topic as a string.
     *
     * @return An array where the first element is the topic prefix (interpreted as device ID)
     * and the second is the sensor name.
     */
    @java.lang.SuppressWarnings({"squid:S1168"})
    private String[] parseTopic(String source) {

        // That "not a sensor" debug statement below will drive the disk into the ground, better avoid it if possible
        if (seenAlienTopic.contains(source)) {

            // Trace is rarely enabled, no big deal
            logger.trace("seen '{}' already, not matching", source);

            // VT: NOTE: squid:S1168 I'm not going to waste memory to indicate a "skip" condition
            return null;
        }

        // The typical ESPHome topic will look like this:
        //
        // ${ESPHome-topic-prefix}/sensor/${ESPHome-sensor-name}/state

        Matcher m = p.matcher(source);
        m.find();

        if (!m.matches()) {

            logger.debug("{}: not a sensor (this message will repeat once per run)", source);

            // We don't want to see this message again
            seenAlienTopic.add(source);

            // VT: NOTE: squid:S1168 I'm not going to waste memory to indicate a "skip" condition
            return null;
        }

        return new String[] { m.group("deviceId"), m.group("sensorName")};
    }
}
