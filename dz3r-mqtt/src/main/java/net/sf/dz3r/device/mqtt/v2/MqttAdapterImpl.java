package net.sf.dz3r.device.mqtt.v2;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import net.sf.dz3r.device.mqtt.MqttAdapter;
import net.sf.dz3r.device.mqtt.v1.MqttEndpoint;
import net.sf.dz3r.instrumentation.Marker;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.apache.logging.log4j.Level.DEBUG;

/**
 * Read/write MQTT v5 adapter, v2.
 *
 * Unlike {@link MqttListenerImpl} which just listens to an MQTT stream, this class supports sending MQTT messages.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class MqttAdapterImpl extends MqttListenerImpl implements MqttAdapter {

    private Sinks.Many<Mqtt5Publish> sendSink;
    private Flux<Mqtt5Publish> sendFlux;

    public MqttAdapterImpl(MqttEndpoint address) {
        this(address, null, null, false, DEFAULT_CACHE_AGE);
    }

    public MqttAdapterImpl(MqttEndpoint address, String username, String password, boolean autoReconnect) {
        this(address, username, password, autoReconnect, DEFAULT_CACHE_AGE);
    }

    public MqttAdapterImpl(MqttEndpoint address, String username, String password, boolean autoReconnect, Duration cacheFor) {
        super(address, username, password, autoReconnect, cacheFor);


    }

    @Override
    public void publish(String topic, String payload, MqttQos qos, boolean retain) {


        ThreadContext.push("publish");
        Marker m = new Marker("publish", DEBUG);

        try {

            var message = Mqtt5Publish
                    .builder()
                    .topic(topic)
                    .payload(payload == null ? null : payload.getBytes(StandardCharsets.UTF_8))
                    .qos(qos)
                    .retain(retain)
                    .build();

            logger.trace("{}: {}", getAddress(), message);
            getSink().tryEmitNext(message);

        } finally {
            m.close();
            ThreadContext.pop();
        }
    }

    private synchronized Sinks.Many<Mqtt5Publish> getSink() {

        if (sendSink != null) {
            return sendSink;
        }

        sendSink = Sinks.many().unicast().onBackpressureBuffer();
        sendFlux = sendSink.asFlux();

        getClient()
                .publish(sendFlux)
                .publishOn(Schedulers.newSingle("mqtt-adapter-" + getAddress()))
                .doOnNext(ack -> logger.trace("{}: ack={}", getAddress(), ack))
                .subscribe();

        return sendSink;
    }
}
