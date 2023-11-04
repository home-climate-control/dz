package net.sf.dz3r.device.mqtt;

import net.sf.dz3r.device.Addressable;
import net.sf.dz3r.device.mqtt.v1.MqttEndpoint;
import net.sf.dz3r.device.mqtt.v1.MqttSignal;
import reactor.core.publisher.Flux;

/**
 * MQTT stream publisher.
 *
 * Doesn't implement the {@link net.sf.dz3r.signal.SignalSource} interface - no need at this point
 * in the data pipeline, DZ entities haven't been resolved yet.
 *
 * If you need to publish messages, use {@link MqttAdapter} instead.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public interface MqttListener extends Addressable<MqttEndpoint>, AutoCloseable {

    /**
     * Get an MQTT topic flux.
     *
     * @param topic Root topic to get the flux for.
     * @param includeSubtopics Self-explanatory.
     *
     * @return Topic flux. Contains everything in subtopics as well if so ordered.
     */
    Flux<MqttSignal> getFlux(String topic, boolean includeSubtopics);
}
