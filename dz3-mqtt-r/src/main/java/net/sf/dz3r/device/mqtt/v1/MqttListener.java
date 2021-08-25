package net.sf.dz3r.device.mqtt.v1;

import com.hivemq.client.mqtt.datatypes.MqttTopic;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.exceptions.Mqtt3ConnAckException;
import net.sf.dz3r.device.Addressable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * MQTT stream cold publisher.
 *
 * Doesn't implement the {@link net.sf.dz3r.signal.SignalSource} interface - no need at this point,
 * DZ entities haven't been resolved yet.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class MqttListener implements Addressable<MqttEndpoint> {

    private final Logger logger = LogManager.getLogger();

    public final MqttEndpoint address;
    private final String username;
    private final String password;

    /**
     * MQTT client.
     *
     * Note that the client is the MQTT v.3 client - this is what {@code mosquitto} supports.
     * May be upgraded to {@link com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient} when a suitable
     * broker replacement is found and verified.
     */
    private final Mqtt3AsyncClient client;

    private final Map<String, Flux<MqttSignal>> topic2flux = new TreeMap<>();

    /**
     * Create an unauthenticated instance that will NOT automatically reconnect.
     *
     * @param address MQTT broker endpoint.
     */
    public MqttListener(MqttEndpoint address) {
        this(address, null, null, false);
    }

    public MqttListener(MqttEndpoint address, String username, String password, boolean autoReconnect) {

        this.address = address;
        this.username = username;
        this.password = password;

        logger.info("Endpoint: {}", address);

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

            logger.info("Connecting to {} (disable reconnect if this gets stuck)", address);
            var ack = instance.send();

            // send() throws an exception upon failure, will this ever be anything other than SUCCESS?
            logger.info("Connected: {}", ack.getReturnCode());

        } catch (Mqtt3ConnAckException ex) {
            throw new IllegalStateException("Can't connect to " + address, ex);
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
     * @see #createFlux(String)
     */
    public synchronized Flux<MqttSignal> getFlux(String topic) {
        logger.info("getFlux: {}", topic);
        return topic2flux.computeIfAbsent(topic, k -> createFlux(topic));
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
     * @see #getFlux(String)
     */
    private Flux<MqttSignal> createFlux(String topic) {

        logger.info("createFlux: topic={}", topic);

        Flux<MqttSignal> result = Flux.create(sink -> {
            logger.debug("New receiver, topic={}", topic);
            register(topic, (mqttTopic, payload) -> {
                logger.trace("receive: {} {}", mqttTopic, payload);
                sink.next(new MqttSignal(mqttTopic.toString(), payload));
            });
        });

        var ackFuture = client.toAsync()
                .subscribeWith()
                .topicFilter(topic + "/#")
                .callback(p -> {

                    // This will be null until someone calls subscribe() on the flux
                    var r = topic2receiver.get(topic);

                    if (r == null) {

                        // Persistent messages will be delivered immediately before there's a chance to call
                        // subscribe() on the flux - we didn't even return it yet

                        // VT: FIXME: Buffer the last value received? May want to log this once per runtime, logs of chatter here

                        logger.debug("no subscriptions to '{}/#' yet, dropped: {} {}", topic, p.getTopic(), new String(p.getPayloadAsBytes(), StandardCharsets.UTF_8));
                        return;
                    }

                    r.receive(p.getTopic(), new String(p.getPayloadAsBytes(), StandardCharsets.UTF_8));

                })
                .send();
        logger.info("Subscribing...");

        try {

            var ack = ackFuture.get();
            logger.info("Subscribed: {}", ack.getReturnCodes());

            topic2flux.put(topic, result);

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

    private interface Receiver {
        public void receive(MqttTopic topic, String payload);
    }
}
