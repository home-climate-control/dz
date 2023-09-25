package net.sf.dz3r.device.esphome.v1;

import net.sf.dz3r.common.HCCObjects;
import net.sf.dz3r.device.Addressable;
import net.sf.dz3r.device.mqtt.v1.MqttEndpoint;
import net.sf.dz3r.device.mqtt.v1.MqttListener;
import net.sf.dz3r.device.mqtt.v1.MqttSignal;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.SignalSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;

import java.time.Instant;

public class ESPHomeListener implements Addressable<MqttEndpoint>, SignalSource<String, Double, Void> {

    protected final Logger logger = LogManager.getLogger();

    private final MqttListener mqttListener;
    private final String mqttRootTopicSub;

    public ESPHomeListener(MqttListener mqttListener, String mqttRootTopicSub) {

        this.mqttListener = HCCObjects.requireNonNull(mqttListener, "mqttListener can't be null");
        this.mqttRootTopicSub = HCCObjects.requireNonNull(mqttRootTopicSub, "mqttRootTopicSub can't be null");
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

        return mqttListener
                .getFlux(mqttRootTopicSub, true)
                .filter(e -> matchSensorAddress(e, address))
                .doOnNext(s -> logger.debug("{}: matched: {} {}", address, s.topic, s.message))
                .map(this::mqtt2sensor);
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
