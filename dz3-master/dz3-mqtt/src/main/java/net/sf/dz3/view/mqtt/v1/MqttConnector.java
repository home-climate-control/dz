package net.sf.dz3.view.mqtt.v1;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.logging.log4j.ThreadContext;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import net.sf.dz3.device.model.impl.ThermostatModel;
import net.sf.dz3.device.sensor.AnalogSensor;
import net.sf.dz3.device.sensor.Switch;
import net.sf.dz3.instrumentation.Marker;
import net.sf.dz3.scheduler.Scheduler;
import net.sf.dz3.view.Connector;
import net.sf.dz3.view.ConnectorFactory;
import net.sf.jukebox.jmx.JmxDescriptor;

/**
 * MQTT broker interface.
 * 
 * This object is supposed to be instantiated via Spring configuration file, with objects
 * that are supposed to be rendered and/or controlled being present in a set passed to the constructor.
 * 
 * See {@code net.sf.dz3.view.swing.Console} for more information.
 * 
 * {@code init-method="start"} attribute must be used in Spring bean definition, otherwise
 * the connector will not work.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2019
 */
public class MqttConnector extends Connector<JsonRenderer> {

    private static final int MQTT_DEFAULT_PORT = 1883;

    /**
     * VT: FIXME: Provide an ability to generate and keep a persistent UUID
     */
    private final String publisherId = UUID.randomUUID().toString();
    private IMqttClient publisher;

    private final String mqttBrokerHost;
    private final int mqttBrokerPort;
    private final String mqttBrokerUsername;
    private final String mqttBrokerPassword;

    /**
     * Root topic for publishing. Can't be the same as the topic for subscriptions.
     */
    private final String mqttRootTopicPub;

    /**
     * Root topic for subscriptions. Can't be the same as the topic for publishing.
     */
    private final String mqttRootTopicSub;

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
    private final int QOS = 0;

    private final BlockingQueue<UpstreamBlock> upstreamQueue = new LinkedBlockingQueue<UpstreamBlock>();

    private Thread exchanger;

    /**
     * Unauthenticated constructor with a default port.
     *
     * @param mqttBrokerHost Host to connect to.
     * @param mqttRootTopicPub Root topic to publish to.
     * @param mqttRootTopicSub Root topic to subscribe to.
     * @param initSet Entities to publish the status of.
     */
    public MqttConnector(
            String mqttBrokerHost,
            String mqttRootTopicPub, String mqttRootTopicSub,
            Set<Object> initSet) {

        this(mqttBrokerHost, MQTT_DEFAULT_PORT, null, null, mqttRootTopicPub, mqttRootTopicSub, initSet, null);
    }

    /**
     * Unauthenticated constructor with a custom port.
     *
     * @param mqttBrokerHost Host to connect to.
     * @param mqttBrokerPort Port to connect to.
     * @param mqttRootTopicPub Root topic to publish to.
     * @param mqttRootTopicSub Root topic to subscribe to.
     * @param initSet Entities to publish the status of.
     */
    public MqttConnector(
            String mqttBrokerHost, int mqttBrokerPort,
            String mqttRootTopicPub, String mqttRootTopicSub,
            Set<Object> initSet) {

        this(mqttBrokerHost, mqttBrokerPort, null, null, mqttRootTopicPub, mqttRootTopicSub, initSet, null);
    }

    /**
     * Authenticated constructor with a default port.
     *
     * @param mqttBrokerHost Host to connect to.
     * @param mqttBrokerPort Port to connect to.
     * @param mqttBrokerUsername MQTT broker username.
     * @param mqttBrokerPassword MQTT broker password.
     * @param mqttRootTopicPub Root topic to publish to.
     * @param mqttRootTopicSub Root topic to subscribe to.
     * @param initSet Entities to publish the status of.
     */
    public MqttConnector(
            String mqttBrokerHost,
            String mqttBrokerUsername, String mqttBrokerPassword,
            String mqttRootTopicPub, String mqttRootTopicSub,
            Set<Object> initSet) {

        this(mqttBrokerHost, MQTT_DEFAULT_PORT, mqttBrokerUsername, mqttBrokerPassword, mqttRootTopicPub, mqttRootTopicSub, initSet, null);
    }

    /**
     * Authenticated constructor with a custom port.
     *
     * @param mqttBrokerHost Host to connect to.
     * @param mqttBrokerPort Port to connect to.
     * @param mqttBrokerUsername MQTT broker username.
     * @param mqttBrokerPassword MQTT broker password.
     * @param mqttRootTopicPub Root topic to publish to.
     * @param mqttRootTopicSub Root topic to subscribe to.
     * @param initSet Entities to publish the status of.
     */
    public MqttConnector(
            String mqttBrokerHost, int mqttBrokerPort,
            String mqttBrokerUsername, String mqttBrokerPassword,
            String mqttRootTopicPub, String mqttRootTopicSub,
            Set<Object> initSet) {

        this(mqttBrokerHost, mqttBrokerPort, mqttBrokerUsername, mqttBrokerPassword, mqttRootTopicPub, mqttRootTopicSub, initSet, null);
    }

    /**
     * Authenticated constructor with a default port and custom factory set.
     *
     * @param mqttBrokerHost Host to connect to.
     * @param mqttBrokerUsername MQTT broker username.
     * @param mqttBrokerPassword MQTT broker password.
     * @param mqttRootTopicPub Root topic to publish to.
     * @param mqttRootTopicSub Root topic to subscribe to.
     * @param initSet Entities to publish the status of.
     * @param factorySet Set of component connector factories.
     */
    public MqttConnector(
            String mqttBrokerHost,
            String mqttBrokerUsername, String mqttBrokerPassword,
            String mqttRootTopicPub, String mqttRootTopicSub,
            Set<Object> initSet,
            Set<ConnectorFactory<JsonRenderer>> factorySet) {

        this(mqttBrokerHost, MQTT_DEFAULT_PORT, mqttBrokerUsername, mqttBrokerPassword, mqttRootTopicPub, mqttRootTopicSub, initSet, null);
    }

    /**
     * Authenticated constructor with a default port and custom factory set.
     *
     * @param mqttBrokerHost Host to connect to.
     * @param mqttBrokerPort Port to connect to.
     * @param mqttBrokerUsername MQTT broker username.
     * @param mqttBrokerPassword MQTT broker password.
     * @param mqttRootTopicPub Root topic to publish to.
     * @param mqttRootTopicSub Root topic to subscribe to.
     * @param initSet Entities to publish the status of.
     * @param factorySet Set of component connector factories.
     */
    public MqttConnector(
            String mqttBrokerHost, int mqttBrokerPort,
            String mqttBrokerUsername, String mqttBrokerPassword,
            String mqttRootTopicPub, String mqttRootTopicSub,
            Set<Object> initSet,
            Set<ConnectorFactory<JsonRenderer>> factorySet) {

        super(initSet, factorySet);

        this.mqttBrokerHost = mqttBrokerHost;
        this.mqttBrokerPort = mqttBrokerPort;
        this.mqttBrokerUsername = mqttBrokerUsername;
        this.mqttBrokerPassword = mqttBrokerPassword;
        this.mqttRootTopicPub = mqttRootTopicPub;
        this.mqttRootTopicSub = mqttRootTopicSub;

        checkTopics(mqttRootTopicPub, mqttRootTopicSub);

        register(AnalogSensor.class, new SensorFactory());
        register(Switch.class, new SwitchFactory());
        register(ThermostatModel.class, new ThermostatFactory(resolveScheduler(initSet)));
    }


    /**
     * Make sure topic name combination is sane.
     *
     * @param topicPub Name of the topic to publish to.
     * @param topicSub Name of the topic to subscribe to.
     */
    private void checkTopics(String topicPub, String topicSub) {

        // Only one of the topics needs to be present; but both being null doesn't make sense -
        // might just as well remove the object from the configuration altogether

        if (topicPub == null && topicSub == null) {
            throw new IllegalArgumentException("both publishing and subscription topics are nulls");
        }

        // Same topic name for publishing and subscriptions is likely to cause a runaway loop

        if (topicPub != null && topicPub.equals(topicSub)) {
            throw new IllegalArgumentException(
                    "same topic (" + topicPub + ") for both publishing and subscription, " +
                    "runaway loop is likely");
        }
    }

    private Scheduler resolveScheduler(Set<Object> source) {

        for (Iterator<Object> i = source.iterator(); i.hasNext(); ) {

            Object item = i.next();

            if (item instanceof Scheduler) {

                logger.info("Using scheduler: " + item);
                return (Scheduler) item;
            }
        }

        logger.warn("No scheduler provided, no schedule deviations will be reported to remote controls");

        return null;
    }

    @Override
    public JmxDescriptor getJmxDescriptor() {

        return new JmxDescriptor(
                "dz",
                getClass().getSimpleName(),
                mqttBrokerHost
                + (mqttBrokerPort == MQTT_DEFAULT_PORT ? "" : " port " + mqttBrokerPort)
                + " topic " + mqttRootTopicPub,
                "MQTT Connector v1");
    }

    @Override
    protected Map<String, Object> createContext() {

        Map<String, Object> context = new TreeMap<String, Object>();

        context.put(QueueFeeder.QUEUE_KEY, upstreamQueue);
        return context;
    }

    @Override
    protected synchronized void activate2() {

        try {

            startExchanger();
            startMqtt();

        } catch (Throwable t) {

            throw new IllegalStateException("failed to start", t);
        }
    }

    private void startExchanger() {

        exchanger = new Thread(new UpstreamBlockExchanger(mqttBrokerHost, mqttBrokerPort, mqttRootTopicPub, upstreamQueue));

        exchanger.start();
    }

    private void startMqtt() throws MqttException {

        ThreadContext.push("startMqtt");
        Marker m = new Marker("startMqtt");

        try {

            /* only authenticate if both credentials are present */
            if (mqttBrokerUsername != null && mqttBrokerPassword != null) {
                publisher = new MqttClient("tcp://" + mqttBrokerUsername + ":" + mqttBrokerPassword + "@" + mqttBrokerHost + ":" + mqttBrokerPort, publisherId);
            } else {
                if (mqttBrokerUsername != null) {
                    // Bad idea to have no password
                    logger.warn("Missing MQTT password, connecting unauthenticated. This behavior will not be allowed in future releases.");
                }
                publisher = new MqttClient("tcp://" + mqttBrokerHost + ":" + mqttBrokerPort, publisherId);
            }

            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            publisher.connect(options);

        } finally {

            m.close();
            ThreadContext.pop();
        }
    }

    @Override
    protected synchronized void deactivate2() {

        ThreadContext.push("deactivate2");

        try {

            exchanger.interrupt();
            publisher.disconnect();

        } catch (MqttException ex) {

            logger.error("can't disconnect, nor can do anything about it", ex);
        } finally {
            ThreadContext.pop();
        }
    }

    private class UpstreamBlockExchanger extends ImmediateExchanger<UpstreamBlock> {

        private final String mqttRootTopicPub;

        public UpstreamBlockExchanger(
                String mqttBrokerHost, int mqttBrokerPort,
                String mqttRootTopicPub,
                BlockingQueue<UpstreamBlock> upstreamQueue) {
            super(upstreamQueue);

            this.mqttRootTopicPub = mqttRootTopicPub;
        }

        @Override
        protected void send(UpstreamBlock dataBlock) throws IOException {

            ThreadContext.push("send");

            try {

                MqttMessage message = new MqttMessage(dataBlock.payload.getBytes());

                message.setQos(QOS);
                message.setRetained(true);

                publisher.publish(mqttRootTopicPub + "/" + dataBlock.topic, message);

                logger.debug(mqttRootTopicPub + "/" + dataBlock.topic + ": " + dataBlock.payload);

            } catch (MqttException ex) {

                logger.error("can't publish, ignored, is this a persistent error?", ex);

            } finally {
                ThreadContext.pop();
            }
        }
    }
}
