package net.sf.dz3r.device.mqtt.v1;

import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.exceptions.Mqtt3ConnAckException;
import net.sf.dz3r.device.Addressable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

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

    private final Mqtt3AsyncClient client;

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
                    .password(Optional.ofNullable(password).orElse("").getBytes(StandardCharsets.UTF_8))
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

    Flux<MqttSignal> getFlux(String topic) {
        throw new UnsupportedOperationException("Not Implemented");
    }
}
