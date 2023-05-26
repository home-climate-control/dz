package net.sf.dz3r.device.esphome.v1;

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

public class ESPHomeListener implements Addressable<MqttEndpoint>, SignalSource<String, Double, Void> {

    protected final Logger logger = LogManager.getLogger();

    /**
     * How long to wait before reporting a timeout.
     */
    private Duration timeout = Duration.ofSeconds(30);

    private final MqttListener mqttListener;
    private final String mqttRootTopicSub;

    public ESPHomeListener(MqttListener mqttListener, String mqttRootTopicSub) {

        this.mqttListener = mqttListener;
        this.mqttRootTopicSub = mqttRootTopicSub;
    }

    /**
     * Create an instance.
     *
     * Even though deprecated, left intact not to disrupt existing configurations until
     * <a href="https://github.com/home-climate-control/dz/issues/47">issue 47</a> is complete.
     *
     * @deprecated Use {@link ESPHomeListener#ESPHomeListener(MqttListener, String)} instead.
     */
    @Deprecated(forRemoval = false)
    public ESPHomeListener(String host, String mqttRootTopicSub) {
        this(host, MqttEndpoint.DEFAULT_PORT, null, null, false, mqttRootTopicSub);
    }

    /**
     * Create an instance.
     *
     * Even though deprecated, left intact not to disrupt existing configurations until
     * <a href="https://github.com/home-climate-control/dz/issues/47">issue 47</a> is complete.
     *
     * @deprecated Use {@link ESPHomeListener#ESPHomeListener(MqttListener, String)} instead.
     */
    @Deprecated(forRemoval = false)
    public ESPHomeListener(String host, int port,
                           String username, String password,
                           boolean autoReconnect,
                           String mqttRootTopicSub) {

        mqttListener = new MqttListener(new MqttEndpoint(host, port), username, password, autoReconnect);
        this.mqttRootTopicSub = mqttRootTopicSub;
    }

    @Override
    public MqttEndpoint getAddress() {
        return mqttListener.address;
    }

    /**
     * Create an ESPHome sensor flux.
     *
     * @param address Sensor address create the flux for.
     *
     * @return Sensor data flux.
     */
    @Override
    public Flux<Signal<Double, Void>> getFlux(String address) {

        logger.info("getFlux: {}", address);

        return new TimeoutGuard<Double, Void>(timeout)
                .compute(mqttListener
                        .getFlux(mqttRootTopicSub, true)
                        .filter(e -> matchSensorAddress(e, address))
                        .doOnNext(s -> logger.debug("matched: {} {}", s.topic, s.message))
                        .map(this::mqtt2sensor));
    }

    private boolean matchSensorAddress(MqttSignal signal, String address) {
        return signal.topic.endsWith("sensor/" + address + "/state");
    }

    private Signal<Double, Void> mqtt2sensor(MqttSignal mqttSignal) {

        // This is ESPHome, they don't provide timestamps.
        // It is possible to get a stale value if LWT wasn't set up correctly

        var timestamp = Instant.now(); // NOSONAR false positive

        try {

            return new Signal<>(
                    timestamp,
                    Double.parseDouble(mqttSignal.message));

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
