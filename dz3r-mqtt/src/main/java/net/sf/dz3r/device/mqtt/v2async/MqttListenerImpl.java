package net.sf.dz3r.device.mqtt.v2async;

import com.hivemq.client.mqtt.mqtt3.exceptions.Mqtt3ConnAckException;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.suback.Mqtt5SubAck;
import net.sf.dz3r.device.mqtt.v1.MqttEndpoint;
import net.sf.dz3r.device.mqtt.v1.MqttSignal;
import net.sf.dz3r.device.mqtt.v2.AbstractMqttListener;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MQTT v5 stream publisher, v2.
 *
 * This class is using
 * <a href="https://www.hivemq.com/article/mqtt-client-api/the-hivemq-mqtt-client-library-for-java-and-its-async-api-flavor/">HiveMQ MQTT Async API</a>.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class MqttListenerImpl extends AbstractMqttListener {

    private final AtomicInteger clientAccessCount = new AtomicInteger();
    /**
     * This sink
     */
    private final Sinks.One<Mqtt5AsyncClient> clientSink = Sinks.one();

    private final Map<String, Flux<MqttSignal>> topic2flux = new TreeMap<>();

    public MqttListenerImpl(MqttEndpoint address) {
        this(address, null, null, false, DEFAULT_CACHE_AGE);
    }

    public MqttListenerImpl(MqttEndpoint address, String username, String password, boolean autoReconnect, Duration cacheFor) {
        super(address, username, password, autoReconnect, cacheFor);
    }

    @Override
    public Flux<MqttSignal> getFlux(String topic, boolean includeSubtopics) {

        var client = Flux
                .just(0)
                .flatMap(ignored -> getClient());

        return Flux
                .zip(
                        client,
                        Flux.just(topic)
                )
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(client2topic -> logger.info("getFlux: {}{}", client2topic.getT2(), includeSubtopics ? " +subtopics" : ""))
                .flatMap(client2topic -> {
                    synchronized (topic2flux) {
                        return topic2flux.computeIfAbsent(client2topic.getT2(), k -> createFlux(client2topic.getT1(), client2topic.getT2(), includeSubtopics));
                    }
                });
    }

    private Flux<MqttSignal> createFlux(Mqtt5AsyncClient client, String topic, boolean includeSubtopics) {

        logger.info("{}: createFlux: topic={}{}", getAddress(), topic, includeSubtopics ? "/..." : "");

        var start = Instant.now();
        var topicFilter = topic + (includeSubtopics ? "/#" : "");

        Sinks.Many<MqttSignal> topicSink = Sinks.many().multicast().onBackpressureBuffer();

        // https://github.com/home-climate-control/dz/issues/296
        // Caching *everything* is a bit wasteful; let's see how wasteful it is and see if a homegrown solution is needed

        var topicFlux = topicSink.asFlux().cache(cacheFor);

        var ackFuture = client
                .subscribeWith()
                .topicFilter(topicFilter)
                .callback(message -> callback(topicSink, message))
                .send();

        // The rest of this doesn't have to be happening in the same thread, it's mostly diagnostics

        new Thread(() -> {
            logger.info("{}: subscribing to {} ...", getAddress(), topicFilter);
            Mqtt5SubAck ack = null;
            try {
                ack = ackFuture.get();
            } catch (Exception ex) {
                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                logger.error("{}: failed to subscribe to {}", getAddress(), topicFilter, ex);
                topicSink.tryEmitError(ex);
            }
            logger.info("{}: subscribed to {}: {} (took {}ms)", getAddress(), topicFilter, ack, Duration.between(start, Instant.now()).toMillis());

        }).start();

        return topicFlux;
    }

    private void callback(Sinks.Many<MqttSignal> sink, Mqtt5Publish message) {

        var topic = message.getTopic().toString();
        var payload = new String(message.getPayloadAsBytes());

        logger.trace("{}: receive: {} {}", getAddress(), message, payload);
        sink.tryEmitNext(new MqttSignal(topic, payload));
    }

    /**
     * Get the client, or {@link Mono#error(Throwable) fail}.
     */
    private synchronized Mono<Mqtt5AsyncClient> getClient() {

        if (clientAccessCount.getAndIncrement() == 0) {
            new Thread(this::createClient).start();
        }

        return clientSink.asMono();
    }

    /**
     * Create {@link #getClient()}, unconditionally.
     *
     * It is assumed that {@link #clientAccessCount} is guarding the instance and will prevent redundant instantiations.
     */
    private void createClient() {

        ThreadContext.push("createClient");

        try {

            // VT: NOTE: Automatic reconnect is disabled by default, here's why:
            // https://github.com/hivemq/hivemq-mqtt-client/issues/496

            var clientPrototype= Mqtt5Client.builder()
                    .identifier("dz-" + UUID.randomUUID())
                    .serverHost(address.host)
                    .serverPort(address.port);

            if (autoReconnect) {
                clientPrototype = clientPrototype.automaticReconnectWithDefaultConfig();
            }

            var client = clientPrototype.buildAsync();

            var instance = client.toBlocking().connectWith();

            if (username != null && password != null) {
                instance = instance.simpleAuth()
                        .username(username)
                        .password(password.getBytes(StandardCharsets.UTF_8))
                        .applySimpleAuth();
            }

            try {

                logger.info("{}{}: connecting",
                        address,
                        autoReconnect ? " (disable reconnect if this gets stuck)" : "");

                var ack = instance.send();

                // send() throws an exception upon failure, will this ever be anything other than SUCCESS?
                logger.info("{}: connected: {}", getAddress(), ack);

            } catch (Mqtt3ConnAckException ex) {
                throw new IllegalStateException("Can't connect to " + getAddress(), ex);
            }

            clientSink.tryEmitValue(client);

        } catch (Exception ex) {
            clientSink.tryEmitError(ex);
        } finally {
            ThreadContext.pop();
        }
    }
}
