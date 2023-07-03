package net.sf.dz3r.device.zwave.v1;

import net.sf.dz3r.device.Addressable;
import net.sf.dz3r.device.mqtt.v1.MqttEndpoint;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.SignalSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;

public class ZWaveSensorListener implements Addressable<MqttEndpoint>, SignalSource<String, Double, Void> {

    private final MqttEndpoint address;

    private final Logger logger = LogManager.getLogger();

    public ZWaveSensorListener(MqttEndpoint address) {
        this.address = address;
    }

    @Override
    public MqttEndpoint getAddress() {
        return address;
    }

    @Override
    public Flux<Signal<Double, Void>> getFlux(String address) {

        logger.error("NOT IMPLEMENTED: {}#getFlux({}) for {}, returning Empty", getClass().getName(), address, getAddress());
        return Flux.empty();
    }
}
