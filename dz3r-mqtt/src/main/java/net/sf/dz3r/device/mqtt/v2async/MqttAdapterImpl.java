package net.sf.dz3r.device.mqtt.v2async;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import net.sf.dz3r.device.mqtt.MqttAdapter;
import net.sf.dz3r.device.mqtt.v1.MqttEndpoint;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Read/write MQTT v5 adapter, v2.
 *
 * Unlike {@link MqttListenerImpl} which just listens to an MQTT stream, this class supports sending MQTT messages.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class MqttAdapterImpl extends MqttListenerImpl implements MqttAdapter {
    public MqttAdapterImpl(MqttEndpoint address) {
        super(address);
    }

    public MqttAdapterImpl(MqttEndpoint address, String username, String password, boolean autoReconnect, Duration cacheFor) {
        super(address, username, password, autoReconnect, cacheFor);
    }

    @Override
    public void publish(String topic, String payload, MqttQos qos, boolean retain) {

        logger.trace("topic={}, payload={}, qos={}, retain={}", topic, payload, qos, retain);

        var message = Mqtt5Publish
                .builder()
                .topic(topic)
                .payload(payload == null ? null : payload.getBytes(StandardCharsets.UTF_8))
                .qos(qos)
                .retain(retain)
                .build();

        Flux
                .just(0)
                .publishOn(Schedulers.boundedElastic())
                .flatMap(ignored -> getClient())
                .map(client -> client.publish(message))
                .subscribe();
    }
}
