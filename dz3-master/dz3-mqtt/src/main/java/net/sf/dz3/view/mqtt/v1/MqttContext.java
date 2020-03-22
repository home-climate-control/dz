package net.sf.dz3.view.mqtt.v1;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;

import net.sf.dz3.instrumentation.Marker;

public class MqttContext {

    protected final Logger logger = LogManager.getLogger(getClass());

    /**
     * VT: FIXME: Provide an ability to generate and keep a persistent UUID
     */
    public final String clientId = UUID.randomUUID().toString();

    public final String host;
    public final int port;
    public final String username;
    public final String password;

    /**
     * Root topic for publishing. Can't be the same as the topic for subscriptions.
     */
    public final String rootTopicPub;

    /**
     * Root topic for subscriptions. Can't be the same as the topic for publishing.
     */
    public final String rootTopicSub;

    public final MqttCallback callback;

    /**
     * Quality of Service.
     *
     * VT: FIXME: It may be a good idea to make this a constructor argument, to provide the right QOS
     * for the right application (wall dashboard is one thing, control system is totally another).
     *
     * VT: FIXME: QOS for publishing and subscriptions may need to be different.
     *
     * Just a reminder to self,
     *
     * QOS 0 is "at most once"
     * QOS 1 is "at least once"
     * QOS 2 is "exactly once"
     */
    public final int QOS = 0;

    private final IMqttClient client;

    public MqttContext(
            String host, int port,
            String username, String password,
            String rootTopicPub, String rootTopicSub,
            MqttCallback callback) throws MqttException {

        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.rootTopicPub = rootTopicPub;
        this.rootTopicSub = rootTopicSub;
        this.callback = callback;

        /* only authenticate if both credentials are present */
        if (username != null && password != null) {
            client = new MqttClient("tcp://" + username + ":" + password + "@" + host + ":" + port, clientId);
        } else {
            if (username != null) {
                // Bad idea to have no password
                logger.warn("Missing MQTT password, connecting unauthenticated. This behavior will not be allowed in future releases.");
            }
            client = new MqttClient("tcp://" + host + ":" + port, clientId);
        }
    }

    public void start() throws MqttException {

        ThreadContext.push("start");
        Marker m = new Marker("start");

        try {

            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);

            client.setCallback(callback);
            client.connect(options);

            client.subscribe(rootTopicSub, QOS);

        } finally {

            m.close();
            ThreadContext.pop();
        }
    }

    public void publish(String topic, MqttMessage message) throws MqttException, MqttPersistenceException {
        client.publish(topic, message);
    }

    public void disconnect() throws MqttException {
        client.disconnect();
    }

    public void close() throws MqttException {
        client.disconnect();
        client.close();
    }
}