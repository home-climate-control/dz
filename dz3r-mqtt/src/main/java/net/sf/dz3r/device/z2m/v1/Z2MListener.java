package net.sf.dz3r.device.z2m.v1;

import net.sf.dz3r.device.Addressable;
import net.sf.dz3r.device.mqtt.v1.MqttEndpoint;
import net.sf.dz3r.device.mqtt.v1.MqttListener;
import net.sf.dz3r.device.mqtt.v1.MqttSignal;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.SignalSource;
import net.sf.dz3r.signal.filter.TimeoutGuard;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;

/**
 * <a href="https://zigbee2mqtt.io">Zigbee2MQTT</a> sensor stream cold publisher.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2022
 */
public class Z2MListener implements Addressable<MqttEndpoint>, SignalSource<String, String, Void> {

    private final Logger logger = LogManager.getLogger();

    /**
     * How long to wait before reporting a timeout.
     *
     * Behavior of Z2M devices is very different from
     * {@link net.sf.dz3r.device.esphome.v1.ESPHomeListener ESPHome based devices} - they may only report on a value
     * change, timeouts should be much higher, minutes, not seconds.
     *
     * VT: FIXME: Controller logic will not kick in correctly unless the value is delivered at regular short intervals, need a repeater
     */
    private Duration timeout = Duration.ofMinutes(5);

    private final MqttListener mqttListener;
    private final String mqttRootTopicSub;

    public Z2MListener(String host, String mqttRootTopicSub) {
        this(host, MqttEndpoint.DEFAULT_PORT, null, null, false, mqttRootTopicSub);
    }

    public Z2MListener(String host, int port,
                       String username, String password,
                       boolean reconnect,
                       String mqttRootTopicSub) {

        mqttListener = new MqttListener(new MqttEndpoint(host, port), username, password, reconnect, true);
        this.mqttRootTopicSub = mqttRootTopicSub;
    }

    @Override
    public MqttEndpoint getAddress() {
        return mqttListener.address;
    }

    /**
     * Create a Z2M sensor flux.
     *
     * @param address Sensor address create the flux for.
     *
     * @return Sensor data flux.
     */
    @Override
    public Flux<Signal<String, Void>> getFlux(String address) {

        logger.info("getFlux: {} on {}", address, mqttRootTopicSub);

        return new TimeoutGuard<String, Void>(timeout)
                .compute(mqttListener
                        .getFlux(mqttRootTopicSub)
                        .filter(e -> matchSensorAddress(e, address))
                        .doOnNext(s -> logger.debug("matched: {} {}", s.topic, s.message))
                        .map(this::mqtt2sensor));
    }

    private boolean matchSensorAddress(MqttSignal signal, String address) {
        return signal.topic.equals(mqttRootTopicSub + "/" + address);
    }

    private Signal<String, Void> mqtt2sensor(MqttSignal mqttSignal) {

        // This is Z2M, they don't provide timestamps.
        // It is possible to get a stale value if LWT wasn't set up correctly

        var timestamp = Instant.now(); // NOSONAR false positive

        try {

            return new Signal<>(
                    timestamp,
                    mqttSignal.message);

        } catch (Exception ex) {

            // Most probable cause is NaN from the sensor - which is usually a hardware failure, better propagate it

            return new Signal<>(
                    timestamp,
                    null,
                    null,
                    Signal.Status.FAILURE_TOTAL,
                    ex);
        }
    }
}
