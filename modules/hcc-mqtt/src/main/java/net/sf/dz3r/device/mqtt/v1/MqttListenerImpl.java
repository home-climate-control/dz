package net.sf.dz3r.device.mqtt.v1;

import com.hivemq.client.mqtt.datatypes.MqttTopic;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.exceptions.Mqtt3ConnAckException;
import net.sf.dz3r.device.mqtt.MqttListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * MQTT stream cold publisher.
 *
 * Doesn't implement the {@link net.sf.dz3r.signal.SignalSource} interface - no need at this point
 * in the data pipeline, DZ entities haven't been resolved yet.
 *
 * If you need to publish messages, use {@link MqttAdapterImpl} instead.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class MqttListenerImpl implements MqttListener {

    protected final Logger logger = LogManager.getLogger();

    public final MqttEndpoint address;
    private final String username;
    private final String password;
    public final boolean autoReconnect;

    private final Map<String, ReadWriteLock> topic2lock = new LinkedHashMap<>();

    /**
     * MQTT client.
     *
     * Note that the client is the MQTT v.3 client - this is what {@code mosquitto} supports up to Debian/Raspbian Buster.
     * May be upgraded to {@link com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient} when a suitable
     * broker replacement is found and verified, or Bullseye is stable enough.
     *
     * This object must not be accessed directly; use {@link #getClient()} instead.
     */
    private Mqtt3AsyncClient client;

    private final Map<String, Flux<MqttSignal>> topic2flux = new TreeMap<>();

    /**
     * Create an unauthenticated instance that will NOT automatically reconnect.
     *
     * @param address MQTT broker endpoint.
     */
    public MqttListenerImpl(MqttEndpoint address) {
        this(address, null, null, false);
    }

    public MqttListenerImpl(MqttEndpoint address, String username, String password, boolean autoReconnect) {

        this.address = address;
        this.username = username;
        this.password = password;
        this.autoReconnect = autoReconnect;

        logger.info("created endpoint={}, autoReconnect={}", address, autoReconnect);
    }

    protected synchronized Mqtt3AsyncClient getClient() {

        if (client != null) {
            return client;
        }

        ThreadContext.push("getClient");

        try {

            // VT: NOTE: Automatic reconnect is disabled by default, here's why:
            // https://github.com/hivemq/hivemq-mqtt-client/issues/496

            var clientPrototype= Mqtt3Client.builder()
                    .identifier("dz-" + UUID.randomUUID())
                    .serverHost(address.host)
                    .serverPort(address.port);

            if (autoReconnect) {
                clientPrototype = clientPrototype.automaticReconnectWithDefaultConfig();
            }

            client = clientPrototype.buildAsync();

            var instance = client.toBlocking().connectWith();

            if (username != null && password != null) {
                instance = instance.simpleAuth()
                        .username(username)
                        .password(password.getBytes(StandardCharsets.UTF_8))
                        .applySimpleAuth();
            }

            try {

                logger.info("Connecting to {}{}",
                        address,
                        autoReconnect ? " (disable reconnect if this gets stuck)" : "");

                var ack = instance.send();

                // send() throws an exception upon failure, will this ever be anything other than SUCCESS?
                logger.info("Connected: {}", ack.getReturnCode());

            } catch (Mqtt3ConnAckException ex) {
                throw new IllegalStateException("Can't connect to " + address, ex);
            }

            return client;

        } finally {
            ThreadContext.pop();
        }
    }

    @Override
    public MqttEndpoint getAddress() {
        return address;
    }

    /**
     * Get an MQTT topic flux.
     *
     * @param topic Root topic to get the flux for.
     *
     * @return Topic flux. Contains everything in subtopics as well.
     *
     * @see #createFlux(String, boolean)
     */
    public Flux<MqttSignal> getFlux(String topic, boolean includeSubtopics) {

        // Move into a different thread
        return Flux
                .just(topic)
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(t -> logger.info("getFlux: {}{}", t, includeSubtopics ? " +subtopics" : ""))
                .flatMap(t -> {

                    // Simply calling computeIfAbsent() on MQTT flux will cause race conditions because createFlux()
                    // takes over 150ms.

                    // This is somewhat uncool, but a) we can't block() here, but b) must ensure exclusivity, so
                    // the interim solution would be a lock. Good thing is, this is being called in parallel threads
                    // and is not going to matter much.

                    var start = Instant.now();
                    var lock = getLock(t);

                    var acquiredAt = new AtomicLong();
                    lock.lock();
                    try {
                        var now = Instant.now();
                        acquiredAt.set(now.toEpochMilli());
                        logger.debug("acquired lock on topic={} in {}ms", t, Duration.between(start, now).toMillis());

                        // VT: NOTE: ...and after all this trouble it'll still blow up if requests for *different* topics
                        // came at the same time.
                        //
                        // Let's leave it as is for now, it looks like delays are mostly "all or nothing", with rare exceptions.
                        // Need to get back to this someday with a lockless solution, though.

                        synchronized (topic2flux) {
                            return topic2flux.computeIfAbsent(t, k -> createFlux(t, includeSubtopics));
                        }

                    } finally {
                        lock.unlock();
                        logger.debug("released lock on topic={} in {}ms (roundtrip of {}ms)",
                                t,
                                Duration.ofMillis(Instant.now().toEpochMilli() - acquiredAt.get()).toMillis(),
                                Duration.between(start, Instant.now()).toMillis());
                    }
                });
    }

    private synchronized Lock getLock(String topic) {

        // An attempt to simply call computeIfAbsent() will cause the same problem that was originally encountered:
        // ConcurrentModificationException. Overhead here is much smaller, though.

        logger.debug("acquiring lock on topic={} ...", topic);
        return topic2lock.computeIfAbsent(topic, k -> new ReentrantReadWriteLock()).writeLock();
    }

    private final Map<String, Receiver> topic2receiver = new TreeMap<>();

    private void register(String t, Receiver r) {
        topic2receiver.put(t, r);
    }

    /**
     * Create an MQTT topic flux.
     *
     * @param topic Root topic to create the flux for.
     *
     * @return Topic flux. Contains everything in subtopics as well.
     *
     * @see #getFlux(String, boolean)
     */
    private Flux<MqttSignal> createFlux(String topic, boolean includeSubtopics) {

        var start = Instant.now();
        logger.info("createFlux: topic={}{}", topic, includeSubtopics ? "/..." : "");

        Flux<MqttSignal> flux = Flux.create(sink -> {
            logger.debug("New receiver, topic={}", topic);
            register(topic, (mqttTopic, payload) -> {
                logger.trace("receive: {} {}", mqttTopic, payload);
                sink.next(new MqttSignal(mqttTopic.toString(), payload));
            });
        });

        var result = flux
                .doOnNext(s -> logger.trace("{}: receive: {}", getAddress(), s))
                .doOnError(t -> logger.error("{}: errored out", getAddress(), t))
                .doOnComplete(() -> logger.debug("{}: completed", getAddress()))
                .publish()
                .autoConnect();

        var topicFilter = topic + (includeSubtopics ? "/#" : "");

        var ackFuture = getClient()
                .subscribeWith()
                .topicFilter(topicFilter)
                .callback(p -> {

                    // This will be null until someone calls subscribe() on the flux
                    var r = topic2receiver.get(topic);

                    if (r == null) {

                        // Persistent messages will be delivered immediately before there's a chance to call
                        // subscribe() on the flux - we didn't even return it yet

                        // VT: FIXME: Buffer the last value received? May want to log this once per runtime, lots of chatter here
                        // VT: NOTE: This is the root cause for https://github.com/home-climate-control/dz/issues/296; will be handled elsewhere

                        logger.debug("no subscriptions to '{}/#' yet, dropped: {} {}", topic, p.getTopic(), new String(p.getPayloadAsBytes(), StandardCharsets.UTF_8));
                        return;
                    }

                    r.receive(p.getTopic(), new String(p.getPayloadAsBytes(), StandardCharsets.UTF_8));

                })
                .send();
        logger.info("Subscribing to {} ...", topicFilter);

        try {

            var ack = ackFuture.get();
            logger.info("Subscribed to {}: {} (took {}ms)", topicFilter, ack.getReturnCodes(), Duration.between(start, Instant.now()).toMillis());

            return result;

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();

            // Oops. No flux.
            return Flux.error(ex);

        } catch ( ExecutionException ex) {

            // Oops. No flux.
            return Flux.error(ex);
        }
    }

    @Override
    public void close() throws Exception {

        logger.warn("disconnecting {}...", getAddress());
        client.disconnect().get();
        logger.info("disconnected {}", getAddress());
    }

    private interface Receiver {
        public void receive(MqttTopic topic, String payload);
    }
}
