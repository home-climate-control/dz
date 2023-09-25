package net.sf.dz3r.device.z2m.v1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.dz3r.device.Addressable;
import net.sf.dz3r.device.mqtt.v1.MqttAdapter;
import net.sf.dz3r.device.mqtt.v1.MqttEndpoint;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.SignalSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * Generic listener that treats Z2M messages as JSON packets and allows to extract arbitrary measurements from them.
 *
 * Payloads are hardware specific (<a href="https://www.zigbee2mqtt.io/supported-devices/">read more</a>).
 *
 * For example,
 * <a href="https://sonoff.tech/product/gateway-amd-sensors/snzb-02/">SONOFF SNZB-02</a>  provides the following measurements:
 *
 * <ul>
 *     <li>battery</li>
 *     <li>humidity</li>
 *     <li>linkquality</li>
 *     <li>temperature</li>
 *     <li>voltage</li>
 * </ul>
 *
 * Specify the one that you need in the constructor to get the right value in the {@link #getFlux(String) flux}.
 *
 * Read more: <a href="https://www.zigbee2mqtt.io/guide/usage/mqtt_topics_and_messages.html">MQTT Topics and Messages</a>
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2022
 */
public class Z2MJsonListener implements Addressable<MqttEndpoint>, SignalSource<String, Double, Void> {

    private final Logger logger = LogManager.getLogger();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String DEFAULT_MEASUREMENT = "temperature";

    public final String measurement;

    private final Z2MListener z2m;

    public Z2MJsonListener(String host, String mqttRootTopicSub, String measurement) {

        this.measurement = measurement;
        this.z2m = new Z2MListener(host, mqttRootTopicSub);
    }

    public Z2MJsonListener(String host, int port, String username, String password, boolean reconnect, String mqttRootTopicSub, String measurement) {

        this.measurement = measurement;
        this.z2m = new Z2MListener(host, port, username, password, reconnect, mqttRootTopicSub);
    }

    public Z2MJsonListener(MqttAdapter mqttAdapter, String mqttRootTopicSub) {
        this(mqttAdapter, mqttRootTopicSub, null);
    }

    public Z2MJsonListener(MqttAdapter mqttAdapter, String mqttRootTopicSub, String measurement) {

        if (measurement == null) {
            logger.warn("{} {} created with default measurement of {}", mqttAdapter.address, mqttRootTopicSub, DEFAULT_MEASUREMENT);
            measurement = DEFAULT_MEASUREMENT;
        }

        this.measurement = measurement;

        this.z2m = new Z2MListener(mqttAdapter, mqttRootTopicSub);
    }

    @Override
    public MqttEndpoint getAddress() {
        return z2m.getAddress();
    }

    @Override
    public Flux<Signal<Double, Void>> getFlux(String address) {
        return z2m
                .getFlux(address)
                .map(this::convert);
    }

    private Signal<Double, Void> convert(Signal<String, Void> signal) {

        if (signal.isError()) {
            return new Signal<>(signal.timestamp, null, signal.payload, signal.status, signal.error);
        }

        try {

            var payload = objectMapper.readValue(signal.getValue(), Map.class);

            logger.debug("payload: {}", payload);

            var value = Double.parseDouble(String.valueOf(payload.get(measurement)));

            logger.debug("{}={}", measurement, value);

            return new Signal<>(signal.timestamp, value);

        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Can't parse JSON: " + signal.getValue(), e);
        }
    }
}
