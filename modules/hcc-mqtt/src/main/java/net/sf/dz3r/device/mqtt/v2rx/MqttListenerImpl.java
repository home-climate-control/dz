package net.sf.dz3r.device.mqtt.v2rx;

import com.hivemq.client.mqtt.MqttGlobalPublishFilter;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.client.mqtt.mqtt5.reactor.Mqtt5ReactorClient;
import net.sf.dz3r.device.mqtt.v1.MqttEndpoint;
import net.sf.dz3r.device.mqtt.v1.MqttSignal;
import net.sf.dz3r.device.mqtt.v2.AbstractMqttListener;
import net.sf.dz3r.instrumentation.Marker;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static org.apache.logging.log4j.Level.DEBUG;

/**
 * MQTT v5 stream publisher, v2.
 *
 * This class is using
 * <a href="https://www.hivemq.com/article/mqtt-client-api/the-hivemq-mqtt-client-library-for-java-and-its-reactive-api-flavor/">HiveMQ MQTT Reactive API</a>.
 *
 * There is a contradiction between the way the reactive API works (one flux for all subscriptions on a connection),
 * and HCC design goals (minimize the number of active connections). This can't be fixed; there will be several comments below
 * marked with {@code ***} to explain the problem.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class MqttListenerImpl extends AbstractMqttListener {


    private Mqtt5ReactorClient client;

    private final Sinks.Many<Mqtt5Publish> receiveSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Flux<Mqtt5Publish> receiveFlux;
    private final Map<ConnectionKey, Flux<MqttSignal>> topic2flux = Collections.synchronizedMap(new LinkedHashMap<>());

    /**
     * Create an unauthenticated instance that will NOT automatically reconnect.
     *
     * @param address MQTT broker endpoint.
     */
    public MqttListenerImpl(MqttEndpoint address) {
        this(address, null, null, false);
    }

    public MqttListenerImpl(MqttEndpoint address, String username, String password, boolean autoReconnect) {
        this(address, username, password, autoReconnect, DEFAULT_CACHE_AGE);
    }

    public MqttListenerImpl(MqttEndpoint address, String username, String password, boolean autoReconnect, Duration cacheFor) {
        super(address, username, password, autoReconnect, cacheFor);

        receiveFlux = receiveSink.asFlux().cache(cacheFor);

        logger.info("created endpoint={}, autoReconnect={}, cacheFor={}", address, autoReconnect, cacheFor);
    }

    protected final Mqtt5ReactorClient getClient() {

        if (client != null) {
            return client;
        }

        ThreadContext.push("getClient");
        Marker m = new Marker("getClient(" + getAddress() + ")", DEBUG);

        try {

            var stage1 = Mqtt5Client
                    .builder()
                    .identifier("hcc-" + UUID.randomUUID())
                    .serverHost(address.host)
                    .serverPort(address.port);

            var stage2 = autoReconnect
                    ? stage1.automaticReconnectWithDefaultConfig()
                    : stage1;

            // VT: FIXME: Add authentication

            if (username != null || password != null) {
                throw new UnsupportedOperationException("Authentication not implemented, kick the maintainer");
            }

            var stage3 = stage2;

            var stage4 = Mqtt5ReactorClient.from(stage3.buildRx());

            stage4
                    .publishes(MqttGlobalPublishFilter.SUBSCRIBED)
                    .publishOn(Schedulers.newSingle("mqtt-listener-" + getAddress()))

                    // *** Part 1: one flux for all subscriptions per connection. No way to get one flux per subscription.
                    .subscribe(this::receive);

            // VT: FIXME: Hack. Ugly. Remove.
            var gate = new CountDownLatch(1);

            logger.info("Connecting to {}{}",
                    address,
                    autoReconnect ? " (disable reconnect if this gets stuck)" : "");

            // This is just the mono, need to wrap it into a pipeline
            stage4
                    .connect()
                    .doOnSuccess(ack -> logger.debug("{}: connected: {}", getAddress(), ack))
                    .doOnError(error -> {
                        throw new IllegalStateException("Can't connect to " + getAddress(), error);
                    })

                    // VT: FIXME: An extremely ugly, but working hack to avoid transforming the whole call into a Mono right now (there's bigger fish to fry).

                    // This breaks the whole reactive model and is only tolerated here now because it happens once at startup.
                    // Still, need to get rid of this (as well as the 'synchronized' keyword on this method) and replace them
                    // with a lockless solution.
                    .doOnSuccess(ignored -> gate.countDown())

                    // VT: FIXME: This is where the race condition breaking everything is happening - subscribe() leaves the current thread, and block() can't be used here
                    .subscribe(ack -> logger.info("{}: ack={}", getAddress(), ack));

            logger.debug("{}: awaiting at the gate...", getAddress());

            gate.await();

            logger.debug("{}: past the gate", getAddress());

            client = stage4;

            return client;

        } catch (InterruptedException ex) {
            throw new IllegalStateException("We're screwed on " + getAddress(), ex);
        } finally {
            m.close();
            ThreadContext.pop();
        }
    }

    /**
     * Receive ALL the messages for ALL the topics this listener is subscribed to.
     *
     * This is the way {@link Mqtt5ReactorClient#publishes(MqttGlobalPublishFilter)} is implemented - the only ways to receive individual topic fluxes are
     * either to create multiple clients (out of the question, dozens of connections at play here), or
     * to filter topics inside manually. Current implementation of {@link #createFlux(ConnectionKey)} is doing that, clumsily.
     *
     * Looks like this approach needs to be scrapped.
     *
     * @param message Incoming message.
     */
    private void receive(Mqtt5Publish message) {
        logger.trace("{}: receive: {} payload={}", getAddress(), message, new String(message.getPayloadAsBytes()));
        receiveSink.tryEmitNext(message);
    }

    /**
     * Get an MQTT topic flux.
     *
     * Subsequent calls with the same ({@code topic, includeSubtopics}) arguments will return the same flux,
     * cached for the last {@link #cacheFor}.
     *
     * @param topic Root topic to get the flux for.
     * @param includeSubtopics Self-explanatory.
     *
     * @return Topic flux. Contains everything in subtopics as well if so ordered.
     */
    @Override
    public Flux<MqttSignal> getFlux(String topic, boolean includeSubtopics) {
        checkClosed();

        var key = new ConnectionKey(topic, includeSubtopics);

        synchronized (this) {
            return topic2flux
                    .computeIfAbsent(key, k -> createFlux(key));
        }
    }

    private Flux<MqttSignal> createFlux(ConnectionKey key) {

        var topicFilter = key.topic() + (key.includeSubtopics() ? "/#" : "");

        Marker m = new Marker("subscribe " + key);

        try {
            getClient()
                    .subscribeWith()
                    .topicFilter(topicFilter)
                    .applySubscribe()
                    .doOnSubscribe(ack -> logger.debug("{}: subscribed: {}", getAddress(), key))
                    .subscribe();

        } finally {
            m.close();
        }

        return receiveFlux
                .publishOn(Schedulers.boundedElastic())

                // *** Part 2: here, we're forced to make ALL subscription fluxes to filter the single incoming stream,
                // instead of having this done inside the MQTT client where it can be done efficiently.
                // See v1 or v2async implementations for details.
                .filter(message -> {
                    var topic = message.getTopic().toString();
                    var pass = key.includeSubtopics()
                            ? topic.startsWith(key.topic())
                            : topic.equals(key.topic());
                    logger.trace("filter: '{}' vs '{}'{} => {}", topic, key.topic(), key.includeSubtopics() ? " +subtopics" : "", pass);

                    return pass;
                })

                .doOnNext(message -> logger.trace("pass: {}", message))
                .map(message -> new MqttSignal(message.getTopic().toString(), new String(message.getPayloadAsBytes())));
    }

}
