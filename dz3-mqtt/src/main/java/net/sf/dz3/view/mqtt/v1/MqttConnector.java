package net.sf.dz3.view.mqtt.v1;

import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;
import net.sf.dz3.device.model.Thermostat;
import net.sf.dz3.device.model.impl.ThermostatModel;
import net.sf.dz3.device.sensor.Addressable;
import net.sf.dz3.device.sensor.AnalogSensor;
import net.sf.dz3.device.sensor.Switch;
import net.sf.dz3.scheduler.Scheduler;
import net.sf.dz3.view.Connector;
import net.sf.dz3.view.ConnectorFactory;
import org.apache.logging.log4j.ThreadContext;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
 * @see MqttDeviceFactory
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2020
 */
public class MqttConnector extends Connector<JsonRenderer> {

    /**
     * @see SensorRenderer#render(com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample)
     * @see SwitchRenderer#render(com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample)
     * @see ThermostatRenderer#render(com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample)
     */
    static enum EntityType {

        SENSOR,
        SWITCH,
        THERMOSTAT;

        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }
    }

    private final MqttContext mqtt;
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
            Set<Object> initSet) throws MqttException {

        this(mqttBrokerHost, MqttContext.DEFAULT_PORT, null, null, mqttRootTopicPub, mqttRootTopicSub, initSet, null);
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
            Set<Object> initSet) throws MqttException {

        this(mqttBrokerHost, mqttBrokerPort, null, null, mqttRootTopicPub, mqttRootTopicSub, initSet, null);
    }

    /**
     * Authenticated constructor with a default port.
     *
     * @param mqttBrokerHost Host to connect to.
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
            Set<Object> initSet) throws MqttException {

        this(mqttBrokerHost, MqttContext.DEFAULT_PORT, mqttBrokerUsername, mqttBrokerPassword, mqttRootTopicPub, mqttRootTopicSub, initSet, null);
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
            Set<Object> initSet) throws MqttException {

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
            Set<ConnectorFactory<JsonRenderer>> factorySet) throws MqttException {

        this(mqttBrokerHost, MqttContext.DEFAULT_PORT, mqttBrokerUsername, mqttBrokerPassword, mqttRootTopicPub, mqttRootTopicSub, initSet, factorySet);
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
            Set<ConnectorFactory<JsonRenderer>> factorySet) throws MqttException {

        super(initSet, factorySet);

        this.mqtt = new MqttContext(
                mqttBrokerHost, mqttBrokerPort,
                mqttBrokerUsername, mqttBrokerPassword,
                mqttRootTopicPub, mqttRootTopicSub, new Callback());

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
                mqtt.host
                + (mqtt.port == MqttContext.DEFAULT_PORT ? "" : " port " + mqtt.port)
                + " topic/pub " + mqtt.rootTopicPub
                + " topic/sub " + mqtt.rootTopicSub,
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

            mqtt.start();
            startExchanger();

            // Connectors are initialized very late in the game, it is likely that the control logic
            // has been long activated - but it is also likely that there will be no new events for
            // quite a while. Need to send all the data that we have to listeners so they can act on it.

            flush();

        } catch (Throwable t) {

            throw new IllegalStateException("failed to start", t);
        }
    }

    /**
     * Send the status of all {@link #getInitSet() initSet} objects into the transport.
     */
    private void flush() {

        ThreadContext.push("flush");

        try {

            // VT: NOTE: This is an ugly hack. The connector architecture was never intended
            // to poll object status, only to handle events broadcast from upstream.

            for (Iterator<Object> i = getInitSet().iterator(); i.hasNext(); ) {

                Object source = i.next();

                if (source instanceof Switch) {

                    flush((Switch) source);
                    continue;
                }

                // VT: NOTE: Flushing sensor data is not that important, it will be handled normally
                // next time sensors are polled or come up with samples. Thermostats will follow
                // immediately thereafter.

                // VT: FIXME: Other entities (namely, HvacController, HvacDriver, ZoneController, Unit)
                // will need to be flushed eventually, when/if they will be capable to be independently
                // controlled without violating DZ abstractions.

                logger.warn("don't know how to flush: " + source.getClass().getName() + ": " + source);
            }

        } finally {
            ThreadContext.pop();
        }
    }

    private void flush(Switch source) {

        ThreadContext.push("flush");

        try {

            logger.info(source);
            source.setState(source.getState());

        } catch (IOException ex) {
            logger.error("failed to flush, ignored (nothing we can do now): " + source, ex);
        } finally {
            ThreadContext.pop();
        }
    }

    private void startExchanger() {

        exchanger = new Thread(new UpstreamBlockExchanger(mqtt.host, mqtt.port, mqtt.rootTopicPub, upstreamQueue));

        exchanger.start();
    }

    @Override
    protected synchronized void deactivate2() {

        ThreadContext.push("deactivate2");

        try {

            exchanger.interrupt();
            mqtt.disconnect();

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

                message.setQos(mqtt.QOS);
                message.setRetained(true);

                mqtt.publish(mqttRootTopicPub + "/" + dataBlock.topic, message);

                logger.debug(mqttRootTopicPub + "/" + dataBlock.topic + ": " + dataBlock.payload);

            } catch (MqttException ex) {

                logger.error("can't publish, ignored, is this a persistent error?", ex);

            } finally {
                ThreadContext.pop();
            }
        }
    }

    private class Callback implements MqttCallback {

        /**
         * Mapping from HA's {@code entity_id} to {@code friendly_name}
         * (which must match our name so we can match their entity to ours).
         */
        private final Map<String, String> entityId2name = new TreeMap<>();

        /**
         * Mapping from known "friendly name" to our entity.
         * {@code null} value indicates that we know this name, but it is not our entity.
         */
        private final Map<String, Object> name2entity = new TreeMap<>();

        @Override
        public void connectionLost(Throwable cause) {

            // VT: FIXME: Currently, this code does NOT support reconnects.
            logger.fatal("connection to tcp://{}:{} lost, will not reconnect, MQTT interface is now dead", mqtt.host, mqtt.port);
        }

        /**
         * Attempt to parse incoming messages as Home Assistant MQTT event stream.
         *
         * @see <a href="https://www.home-assistant.io/components/mqtt_eventstream/">https://www.home-assistant.io/components/mqtt_eventstream/</a>
         */
        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {

            ThreadContext.push("MQTT/messageArrived");

            try {

                logger.debug("{} {}", topic, message);

                try (JsonReader reader = Json.createReader(new ByteArrayInputStream(message.getPayload()))) {

                    JsonObject payload = reader.readObject();
                    JsonString eventType = payload.getJsonString(MqttContext.JsonTag.EVENT_TYPE.name);

                    // We know and care about two event types at this point:
                    //
                    // call_service: somebody moved a slider
                    // state_changed: something changed

                    switch (eventType.getString()) {

                    case "call_service":

                        callService(payload.getJsonObject(MqttContext.JsonTag.EVENT_DATA.name));
                        return;

                    case "state_changed":

                        stateChanged(payload.getJsonObject(MqttContext.JsonTag.EVENT_DATA.name));
                        return;

                    default:

                        logger.warn("don't know how to handle '{}' event_type", eventType);
                        logger.warn("event_data is: {}", payload.getJsonObject(MqttContext.JsonTag.EVENT_DATA.name));
                    }
                }

            } catch (Throwable t) {

                // VT: NOTE: According to the docs, throwing an exception here will shut down the client - can't afford that,
                // so we'll just complain loudly

                logger.error("MQTT message caused an exception: {}", message, t);

            } finally {
                ThreadContext.pop();
            }
        }

        /**
         * Handle HA's {@code call_service} message.
         *
         * @param eventData HA's {@code event_data} JSON fragment.
         */
        private void callService(JsonObject eventData) {

            ThreadContext.push("callService");

            try {

                logger.info("data: {}", eventData);

                // Since this is a control input, let's be paranoid and verify that it contains what we expect

                String domain = eventData.getJsonString("domain").getString();
                String service = eventData.getJsonString("service").getString();

                if (!"climate".equals(domain) || !"set_temperature".equals(service)) {

                    throw new IllegalArgumentException(
                            "invalid command: expected (climate, set_temperature)" +
                            ", received ("+ domain + ", " + service + ")");
                }

                String entityId = eventData.
                        getJsonObject("service_data").
                        getJsonString("entity_id").getString();
                Object entity = resolveEntity(entityId);

                if (entity == null) {

                    // VT: NOTE: This is expected behavior in case the entity is not ours
                    // we've already logged this at debug level, let's just remind ourselves about that

                    logger.info(entityId + ": not ours; our known map is: " + entityId2name);
                    return;
                }

                logger.info("resolved entity: " + entity);

                if (!(entity instanceof ThermostatModel)) {

                    throw new IllegalStateException("not a thermostat, but " + entity.getClass().getName() + ": " + entity);
                }

                double setpoint = eventData.
                        getJsonObject("service_data").
                        getJsonNumber("temperature").doubleValue();

                logger.info("setpoint: " + setpoint);

                ((ThermostatModel) entity).setSetpoint(setpoint);

            } finally {
                ThreadContext.pop();
            }
        }

        /**
         * Handle HA's {@code call_service} message.
         *
         * We are only interested in this message as it is an echo of the message we sent to it earlier;
         * we will use it to figure out the {@code entity_id} we'll need to recognize later.
         *
         * @param eventData HA's {@code event_data} JSON fragment.
         */
        private void stateChanged(JsonObject eventData) {

            ThreadContext.push("stateChanged");

            try {

                logger.debug("data: " + eventData);

                // We need two elements now:
                //
                // .event_data.entity_id
                // .event_data.new_state.attributes.friendly_name

                String entityId = eventData.getJsonString("entity_id").getString();
                String friendlyName = eventData.
                        getJsonObject("new_state").
                        getJsonObject("attributes").
                        getJsonString("friendly_name").getString();
                String knownName = entityId2name.get(entityId);

                if (entityId2name.get(entityId) == null) {

                    logger.info("new entity discovered: " + entityId + ": " + friendlyName);

                    // Since call_service event will only contain entity_id, we'll need to map it
                    // back to our entity later

                    entityId2name.put(entityId, friendlyName);

                } else if (!knownName.equals(friendlyName)) {

                    logger.warn(entityId + ": old and new name mismatch ('" + knownName + "' vs. '"+ friendlyName + "', ignored");
                }

                Object entity = resolveEntity(entityId);

                if (entity == null) {

                    // VT: NOTE: This is expected behavior in case the entity is not ours;
                    // we've already logged this at debug level. No need to do anything

                    return;
                }

                if (entity instanceof Thermostat) {

                    // VT: NOTE: This must be an echo from our own state change broadcast.
                    // We will only honor callService() for our own thermostats.

                    return;
                }

                // VT: FIXME: handle the message
                logger.error("Not Implemented: " + entity, new IllegalStateException());

            } finally {
                ThreadContext.pop();
            }
        }

        /**
         * Resolve our entity from the ID given.
         *
         * @param entityId Entity ID to resolve.
         *
         * @return Our object corresponding to {@code entity_id} received in {@link #callService(JsonObject)}
         */
        private Object resolveEntity(String entityId) {

            ThreadContext.push("resolveEntity");

            try {

                String entityName = entityId2name.get(entityId);

                if (entityName == null) {

                    logger.warn("unknown entity_id: '" + entityId + "', our known maps are:");
                    logger.warn("id to name: " + entityId2name);
                    logger.warn("name to entity: " + name2entity);

                    throw new IllegalArgumentException("unknown entity_id: '" + entityId + "', did everything settle?");
                }

                Object cached = name2entity.get(entityName);

                if (cached != null) {

                    logger.debug("cached instance: " + entityName);
                    return cached;
                }

                if (name2entity.containsKey(entityName)) {

                    logger.debug("we know about '" + entityName + "', but it isn't ours");
                    return null;
                }

                for (Object entity : getInitSet()) {

                    // VT: NOTE: This is where things get complicated. Our entities were never supposed to be
                    // resolved from outside. See https://github.com/home-climate-control/dz/issues/87 for details.

                    // For now, let's do it piecemeal, and then unify when the issue above is addressed.

                    if (entity instanceof Addressable) {

                        String name = ((Addressable) entity).getAddress();

                        if (name.equals(entityName)) {

                            logger.info("resolved '" + entityId + "' into " + entity);
                            name2entity.put(entityName, entity);

                            return entity;
                        }

                    } else if (entity instanceof Thermostat) {

                        String name = ((Thermostat) entity).getName();

                        if (name.equals(entityName)) {

                            logger.info("resolved '" + entityId + "' into " + entity);
                            name2entity.put(entityName, entity);

                            return entity;
                        }

                    } else {

                        // VT: FIXME: We won't control anything other than thermostats at this point.
                        logger.warn("don't know how to handle: " + entity.getClass().getName());
                    }
                }

                // VT: NOTE: No need to be throwing exceptions here, it may be the entity that is not present
                // in our instance, but is present in our deployment

                logger.warn("unresolved entity: '" + entityId + "', our known map is: " + name2entity);

                // And let's make a note of it
                name2entity.put(entityName, null);

                return null;

            } finally {
                ThreadContext.pop();
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            // VT: NOTE: Nothing to do here
        }
    }
}
