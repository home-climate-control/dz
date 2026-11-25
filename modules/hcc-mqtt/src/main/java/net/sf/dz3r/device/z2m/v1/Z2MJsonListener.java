package net.sf.dz3r.device.z2m.v1;

import com.homeclimatecontrol.hcc.TimeTool;
import com.homeclimatecontrol.hcc.signal.Signal;
import net.sf.dz3r.device.Addressable;
import net.sf.dz3r.device.mqtt.MqttAdapter;
import net.sf.dz3r.device.mqtt.v1.MqttEndpoint;
import net.sf.dz3r.signal.SignalSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;

import static java.lang.Long.parseLong;

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
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2026
 */
public class Z2MJsonListener implements Addressable<MqttEndpoint>, SignalSource<String, Double, Void> {

    private final Logger logger = LogManager.getLogger();
    private final JsonMapper jsonMapper = new JsonMapper();

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
            logger.warn("{} {} created with default measurement of {}", mqttAdapter.getAddress(), mqttRootTopicSub, DEFAULT_MEASUREMENT);
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
            return new Signal<>(signal.timestamp(), null, signal.payload(), signal.status(), signal.error());
        }

        try {

            var payload = jsonMapper.readValue(signal.getValue(), Map.class);

            logger.debug("payload: {}", payload);

            var value = Double.parseDouble(String.valueOf(payload.get(measurement)));

            logger.debug("convert: {}={}", measurement, value);

            var lastSeen = parseLastSeen(logger, String.valueOf(payload.get("last_seen")));
            logger.debug("convert: last_seen={}", lastSeen);

            if (lastSeen != null) {
                logger.debug("last_seen lag: {}", Duration.between(signal.timestamp(), lastSeen));
            }

            return new Signal<>(lastSeen == null ? signal.timestamp() : lastSeen, value);

        } catch (JacksonException | NumberFormatException ex) {

            // Throwing an exception here breaks everything
            // https://github.com/home-climate-control/dz/issues/303

            logger.error("Can't parse JSON:\n{}\n---", signal.getValue(), ex);
            return new Signal<>(signal.timestamp(), null, signal.payload(), Signal.Status.FAILURE_TOTAL, ex);
        }
    }

    /**
     * Parse the {@code last_seen} value per https://www.zigbee2mqtt.io/guide/configuration/all-settings.html#last-seen
     *
     * @param source Raw value obtained from the payload, could be {@code null}.
     *
     * @return Parsed value, or {@code null} if it is missing or unparseable.
     */
    static Instant parseLastSeen(Logger logger, String source) {

        // The value can be:

        // - missing if disabled (default Z2M behavior)
        // - ISO_8601
        // - ISO_8601_local
        // - epoch

        // All these must provide the same behavior - just like it was beore #354.

        try {

            return TimeTool.parseISO_8601(source);

        } catch (DateTimeParseException ex) {

            try {

                // Could be either epoch, or garbage, but not null and not blank
                return Instant.ofEpochMilli(parseLong(source.trim()));

            } catch (Throwable t) {
                logger.warn("Failed to parse '{}' as ISO_8601 or epoch, returning null", source);
                return null;
            }
        }
    }
}
