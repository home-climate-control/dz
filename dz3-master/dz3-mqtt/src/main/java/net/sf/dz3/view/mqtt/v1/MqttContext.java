package net.sf.dz3.view.mqtt.v1;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;

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

    public final IMqttClient client;

    public MqttContext(
            String host, int port,
            String username, String password,
            String rootTopicPub, String rootTopicSub) throws MqttException {

        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.rootTopicPub = rootTopicPub;
        this.rootTopicSub = rootTopicSub;

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
}
