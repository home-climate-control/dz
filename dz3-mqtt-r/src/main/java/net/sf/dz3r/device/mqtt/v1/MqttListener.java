package net.sf.dz3r.device.mqtt.v1;

import net.sf.dz3r.device.Addressable;
import reactor.core.publisher.Flux;

/**
 * MQTT stream cold publisher.
 *
 * Doesn't implement the {@link net.sf.dz3r.signal.SignalSource} interface - no need at this point,
 * DZ entities haven't been resolved yet.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class MqttListener implements Addressable<MqttEndpoint> {

    public final MqttEndpoint address;

    public MqttListener(MqttEndpoint address) {
        this.address = address;
    }

    @Override
    public MqttEndpoint getAddress() {
        return address;
    }

    Flux<MqttSignal> getFlux(String topic) {
        throw new UnsupportedOperationException("Not Implemented");
    }
}
