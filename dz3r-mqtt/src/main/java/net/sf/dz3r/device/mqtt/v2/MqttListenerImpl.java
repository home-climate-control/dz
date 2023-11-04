package net.sf.dz3r.device.mqtt.v2;

import com.hivemq.client.mqtt.MqttGlobalPublishFilter;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.client.mqtt.mqtt5.reactor.Mqtt5ReactorClient;
import net.sf.dz3r.common.HCCObjects;
import net.sf.dz3r.device.mqtt.MqttListener;
import net.sf.dz3r.device.mqtt.v1.MqttEndpoint;
import net.sf.dz3r.device.mqtt.v1.MqttSignal;
import net.sf.dz3r.instrumentation.Marker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.apache.logging.log4j.Level.DEBUG;

/**
 * MQTT v5 stream publisher, v2.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class MqttListenerImpl implements MqttListener {

    public static final Duration DEFAULT_CACHE_AGE = Duration.ofSeconds(30);

    protected final Logger logger = LogManager.getLogger();

    public final MqttEndpoint address;
    private final String username;
    private final String password;
    public final boolean autoReconnect;
    public final Duration cacheFor;

    private boolean closed = false;

    private Mqtt5ReactorClient client;

    private final Sinks.Many<Mqtt5Publish> receiveSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Flux<Mqtt5Publish> receiveFlux;
    private final Map<ConnectionKey, Flux<MqttSignal>> topic2flux = Collections.synchronizedMap(new LinkedHashMap<>());

//    private

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

        this.address = HCCObjects.requireNonNull(address, "address can't be null");
        this.username = username;
        this.password = password;
        this.autoReconnect = autoReconnect;
        this.cacheFor = HCCObjects.requireNonNull(cacheFor, "cacheFor can't be null");

        receiveFlux = receiveSink.asFlux().cache(cacheFor);

        logger.info("created endpoint={}, autoReconnect={}, cacheFor={}", address, autoReconnect, cacheFor);
    }

    private void checkClosed() {
        if (closed) {
            throw new IllegalStateException("Already close()d");
        }
    }

    protected final synchronized Mqtt5ReactorClient getClient() {

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
            var stage3 = stage2;

            client = Mqtt5ReactorClient.from(stage3.buildRx());

            client
                    .publishes(MqttGlobalPublishFilter.ALL)
                    .publishOn(Schedulers.newSingle("mqtt-listener-" + getAddress()))
                    .subscribe(this::receive);

            client
                    .connect()
                    .doOnSuccess(ack -> logger.info("{}: connected: {}", getAddress(), ack))
                    .doOnError(error -> {
                        throw new IllegalStateException("Can't connect to " + getAddress(), error);
                    })
                    .subscribe();

            return client;

        } finally {
            m.close();
            ThreadContext.pop();
        }
    }

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

        var topicFilter = key.topic + (key.includeSubtopics ? "/#" : "");

        Marker m = new Marker("subscribe " + key);

        try {
            getClient()
                    .subscribeWith()
                    .topicFilter(topicFilter)
                    .applySubscribe()
                    .doOnSubscribe(ack -> logger.debug("{}: subscribed: {}", key, ack))
                    .block();

        } finally {
            m.close();
        }

        return receiveFlux
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

    /**
     * Close and mark unusable.
     */
    @Override
    public void close() throws Exception {
        closed = true;
    }

    @Override
    public MqttEndpoint getAddress() {
        return address;
    }

    private record ConnectionKey(
            String topic,
            boolean includeSubtopics
    ) {

    }
}
