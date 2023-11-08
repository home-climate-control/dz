package net.sf.dz3r.device.z2m.v1;

import net.sf.dz3r.device.Addressable;
import net.sf.dz3r.device.mqtt.MqttListener;
import net.sf.dz3r.device.mqtt.v1.MqttEndpoint;
import net.sf.dz3r.device.mqtt.v1.MqttSignal;
import net.sf.dz3r.device.mqtt.v2async.MqttListenerImpl;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.SignalSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;

import java.time.Instant;

import static net.sf.dz3r.device.mqtt.v2.AbstractMqttListener.DEFAULT_CACHE_AGE;

/**
 * <a href="https://zigbee2mqtt.io">Zigbee2MQTT</a> sensor stream cold publisher.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class Z2MListener implements Addressable<MqttEndpoint>, SignalSource<String, String, Void> {

    private final Logger logger = LogManager.getLogger();

    private final MqttListener mqttListener;
    private final String mqttRootTopicSub;

    public Z2MListener(String host, String mqttRootTopicSub) {
        this(host, MqttEndpoint.DEFAULT_PORT, null, null, false, mqttRootTopicSub);
    }

    public Z2MListener(String host, int port,
                       String username, String password,
                       boolean reconnect,
                       String mqttRootTopicSub) {

        mqttListener = new MqttListenerImpl(new MqttEndpoint(host, port), username, password, reconnect, DEFAULT_CACHE_AGE);
        this.mqttRootTopicSub = mqttRootTopicSub;
    }

    public Z2MListener(MqttListener mqttListener, String mqttRootTopicSub) {
        this.mqttListener = mqttListener;
        this.mqttRootTopicSub = mqttRootTopicSub;
    }

    @Override
    public MqttEndpoint getAddress() {
        return mqttListener.getAddress();
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

        return mqttListener
                .getFlux(mqttRootTopicSub, true)
                .filter(e -> matchSensorAddress(e, address))
                .doOnNext(s -> logger.debug("{}: matched: {}", address, s))
                .map(this::mqtt2sensor);
    }

    private boolean matchSensorAddress(MqttSignal signal, String address) {
        return signal.topic().equals(mqttRootTopicSub + "/" + address);
    }

    private Signal<String, Void> mqtt2sensor(MqttSignal mqttSignal) {

        // This is Z2M, they don't provide timestamps.
        // It is possible to get a stale value if LWT wasn't set up correctly

        var timestamp = Instant.now(); // NOSONAR false positive

        try {

            return new Signal<>(
                    timestamp,
                    mqttSignal.message());

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
