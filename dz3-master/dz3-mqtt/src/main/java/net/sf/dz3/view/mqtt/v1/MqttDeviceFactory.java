package net.sf.dz3.view.mqtt.v1;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;

import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import net.sf.dz3.device.sensor.Addressable;
import net.sf.dz3.device.sensor.AnalogSensor;
import net.sf.dz3.device.sensor.DeviceFactory2020;
import net.sf.dz3.device.sensor.Switch;
import net.sf.jukebox.datastream.logger.impl.DataBroadcaster;
import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.datastream.signal.model.DataSink;
import net.sf.jukebox.datastream.signal.model.DataSource;
import net.sf.jukebox.jmx.JmxAware;
import net.sf.jukebox.jmx.JmxDescriptor;

/**
 * Factory for sensors and actuators supported via MQTT.
 *
 * The difference between this class and {@link MqttConnector} is that the
 * latter is *exposing* a set of DZ entities to outside consumers (and providing
 * a feedback mechanism), whereas this class is specifically intended to accept
 * reports from outside entities, and issue commands to them.
 *
 * VT: NOTE: this class is a radical departure from the old, complex
 * implementation of {@code OwapiDeviceFactory}, {@code XBeeDeviceFactory}, the
 * gorilla, and the whole jungle behind - the 20 year old architecture is too
 * complicated for my taste today. Let's see how much simpler it can get while
 * keeping the same design contract.
 *
 * @see MqttConnector
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2020
 */
public class MqttDeviceFactory implements DeviceFactory2020, AutoCloseable, MqttConstants, JmxAware {

    protected final Logger logger = LogManager.getLogger(getClass());

    private final MqttContext mqtt;
    private final Watchdog watchdog;
    private final Thread watchdogThread;
    private final CountDownLatch stopGate = new CountDownLatch(1);

    private static final long POLL_INTERVAL = 10000L;
    private static final long STALE_AGE = POLL_INTERVAL * 5;

    /**
     * Data map.
     *
     * The key is the device address, the value is device itself.
     */
    private final Map<String, Device<?>> deviceMap = new TreeMap<>();

    /**
     * Unauthenticated constructor with a default port.
     *
     * @param mqttBrokerHost Host to connect to.
     * @param mqttRootTopicPub Root topic to publish to.
     * @param mqttRootTopicSub Root topic to subscribe to.
     * @param initSet Entities to publish the status of.
     */
    public MqttDeviceFactory(
            String mqttBrokerHost,
            String mqttRootTopicPub, String mqttRootTopicSub) throws MqttException {

        this(mqttBrokerHost, MQTT_DEFAULT_PORT, null, null, mqttRootTopicPub, mqttRootTopicSub);
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
    public MqttDeviceFactory(
            String mqttBrokerHost, int mqttBrokerPort,
            String mqttRootTopicPub, String mqttRootTopicSub) throws MqttException {

        this(mqttBrokerHost, mqttBrokerPort, null, null, mqttRootTopicPub, mqttRootTopicSub);
    }

    /**
     * Authenticated constructor with a default port.
     *
     * @param mqttBrokerHost Host to connect to.
     * @param port Port to connect to.
     * @param mqttBrokerUsername MQTT broker username.
     * @param mqttBrokerPassword MQTT broker password.
     * @param mqttRootTopicPub Root topic to publish to.
     * @param mqttRootTopicSub Root topic to subscribe to.
     * @param initSet Entities to publish the status of.
     */
    public MqttDeviceFactory(
            String mqttBrokerHost,
            String mqttBrokerUsername, String mqttBrokerPassword,
            String mqttRootTopicPub, String mqttRootTopicSub) throws MqttException {

        this(mqttBrokerHost, MQTT_DEFAULT_PORT, mqttBrokerUsername, mqttBrokerPassword, mqttRootTopicPub, mqttRootTopicSub);
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
    public MqttDeviceFactory(
            String mqttBrokerHost, int mqttBrokerPort,
            String mqttBrokerUsername, String mqttBrokerPassword,
            String mqttRootTopicPub, String mqttRootTopicSub) throws MqttException {

        this.mqtt = new MqttContext(
                mqttBrokerHost, mqttBrokerPort,
                mqttBrokerUsername, mqttBrokerPassword,
                mqttRootTopicPub, mqttRootTopicSub + "/#",
                new Callback());

        mqtt.start();

        // VT: NOTE: Clumsy, but we need access to both the thread (to stop it) and the runnable (to notify())
        watchdog = new Watchdog();
        watchdogThread = new Thread(watchdog);
        watchdogThread.start();
    }

    @Override
    public AnalogSensor getSensor(String address) {
        return new Sensor(address);
    }

    @Override
    public Switch getSwitch(String address) {
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public void close() throws Exception {

        // Watchdog needs to be interrupted first, for when we have remote
        logger.debug("stopping the watchdog...");
        watchdogThread.interrupt();

        stopGate.await();
        logger.debug("watchdog shut down");

        // Some of them may need to send shutdown signal to remote endpoints
        closeDevices();

        // And now we can close the comms.
        mqtt.close();
    }

    private void closeDevices() {
        // VT: NOTE: This will come handy when remote actuators are supported
        logger.warn("closeDevices(): nothing to do at the moment");
    }

    /**
     * Power off.
     *
     * This method should provide an extra level of protection to remote devices
     * that control hardware. If we're smart, the life cycle has already taken care
     * of that, but can't rely on ourselves being smart enough.
     */
    public void powerOff() {
        ThreadContext.push("powerOff");
        try {
            logger.warn("powering off");
            close();
            logger.info("shut down.");
        } catch (Throwable t) {
            logger.fatal("failed to shut down cleanly, better check your hardware", t);
        } finally {
            ThreadContext.pop();
        }
    }

    @Override
    public JmxDescriptor getJmxDescriptor() {
        return new JmxDescriptor(
                "dz",
                getClass().getSimpleName(),
                mqtt.host
                + (mqtt.port == MQTT_DEFAULT_PORT ? "" : " port " + mqtt.port)
                + " topic/pub " + mqtt.rootTopicPub
                + ", topic/sub" + mqtt.rootTopicPub,
                "MqttDeviceFactory v1");
    }


    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void refresh() {
        ThreadContext.push("refresh");
        try {

            logger.debug("heartbeat");

            long now = System.currentTimeMillis();

            for (Device<?> device : deviceMap.values()) {

                DataSample<?> sample = device.getStatus();

                if (now - sample.timestamp > STALE_AGE) {
                    // VT: NOTE: Ideally, this should be synchronized, but practically, the chances are too slim
                    // VT: NOTE: this sample will never contain data of a variable type, hence @SuppressWarnings
                    device.inject(new DataSample(sample.sourceName, sample.signature, null, new Error("stale")));
                }
            }

        } finally {
            ThreadContext.pop();
        }
    }

    static final List<String> MANDATORY_JSON_FIELDS = Arrays.asList(ENTITY_TYPE, NAME, SIGNAL);
    static final List<String> OPTIONAL_JSON_FIELDS = Arrays.asList(TIMESTAMP, SIGNATURE, DEVICE_ID);

    void process(byte[] source) {
        try (JsonReader reader = Json.createReader(new ByteArrayInputStream(source))) {

            JsonObject payload = reader.readObject();

            JsonString entityType = payload.getJsonString(ENTITY_TYPE);
            JsonString name = payload.getJsonString(NAME);
            JsonNumber signal = payload.getJsonNumber(SIGNAL);

            JsonNumber timestamp = payload.getJsonNumber(TIMESTAMP);
            JsonString signature = payload.getJsonString(SIGNATURE);
            JsonString deviceId = payload.getJsonString(DEVICE_ID);

            // We're not using these yet, but let's make sure they're present
            // That'll help figuring out whether we need them in the future.
            checkFields(
                    "optional", Level.DEBUG, OPTIONAL_JSON_FIELDS,
                    new Object[] {
                            timestamp,
                            signature,
                            deviceId
                            });

            if (!checkFields(
                    "mandatory", Level.ERROR, MANDATORY_JSON_FIELDS,
                    new Object[] {
                            entityType,
                            name,
                            signal
                            })) {
                // The check has already complained
                return;
            }

            switch (entityType.getString()) {

            case "sensor":

                processSensorInput(name.getString(), signal.bigDecimalValue(), timestamp, signature, deviceId);
                return;

            default:
                logger.warn("can't process {} yet", entityType.getString());
            }
        }
    }

    /**
     * Check if all the values are present, and complain if they're not.
     *
     * @param reference Fields that are expected to be present.
     * @param source JSON entities parsed out of the payload.
     * @return {@code false} if some of the values are missing.
     */
    boolean checkFields(String context, Level level, List<String> reference, Object[] source) {

        List<String> missing = new LinkedList<>();
        for (int offset = 0; offset < reference.size(); offset++) {
            if (source[offset] == null) {
                missing.add(reference.get(offset));
            }
        }

        ThreadContext.push(context);
        missing
            .stream()
            .forEach(field -> { logger.log(level, "missing: {}", field); });
        ThreadContext.pop();

        return missing.isEmpty();
    }

    void processSensorInput(
            String name,
            BigDecimal signal,
            JsonNumber timestamp,
            JsonString signature,
            JsonString deviceId) {
        ThreadContext.push("processSensorInput");
        try {

            Device<?> d = deviceMap.get(name);

            if (d == null) {
                logger.debug("not ours: {}", name);
                return;
            }

            Sensor s = (Sensor) d;
            double v = signal.doubleValue();
            s.inject(new DataSample<Double>(s.getAddress(), s.getAddress(), v, null));

        } finally {
            ThreadContext.pop();
        }
    }

    private class Callback implements MqttCallback {

        @Override
        public void connectionLost(Throwable cause) {
            throw new IllegalStateException("Not Implemented");
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            ThreadContext.push("MQTT/messageArrived");

            try {

                // VT: NOTE: We ignore topic absolutely at this point other than for logging it
                logger.debug("{} {}", topic, message);

                process(message.getPayload());

            } catch (Throwable t) {

                // VT: NOTE: According to the docs, throwing an exception here will shut down the client - can't afford that,
                // so we'll just complain loudly

                logger.error("MQTT message caused an exception: " + message, t);

            } finally {
                watchdog.release();
                ThreadContext.pop();
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            // VT: NOTE: Nothing to do here
        }
    }

    @java.lang.SuppressWarnings({"squid:S2142","squid:S2189"})
    private class Watchdog implements Runnable {

        @Override
        public synchronized void run() {
            ThreadContext.push("run");
            try {
                while (true) {

                    wait(POLL_INTERVAL);
                    refresh();
                }

            } catch (InterruptedException e) {

                // VT: NOTE: squid:S2142 SonarQube is not smart enough to recognize that the exception *is* handled
                // VT: NOTE: squid:S2189 SonarQube is not smart enough to recognize this as an exit condition
                logger.warn("interrupted, terminating");

            } finally {
                logger.debug("releasing stop gate");
                ThreadContext.clearAll();
                stopGate.countDown();
            }
        }

        @java.lang.SuppressWarnings("squid:S2446")
        public synchronized void release() {
            // VTL NOTE: squid:S2446 There *is* one thread. The watchdog.
            notify();
        }
    }

    private class Device<E> implements DataSource<E>, Addressable {

        protected DataBroadcaster<E> broadcaster = new DataBroadcaster<>();
        private final String address;
        private DataSample<E> status;

        public Device(String address) {

            this.address = address;

            // VT: FIXME: We don't know the actual signature at the moment; let's see if
            // setting to address now and replacing it with the signature received from the
            // MQTT remote later will not break things.

            this.status = new DataSample<>(address, address, null, new IllegalStateException("booting up, unavailable"));

            deviceMap.put(address, this);
        }

        @Override
        public final String getAddress() {
            return address;
        }

        @Override
        public final void addConsumer(DataSink<E> consumer) {
            broadcaster.addConsumer(consumer);
        }

        @Override
        public void removeConsumer(DataSink<E> consumer) {
            broadcaster.removeConsumer(consumer);
        }

        public final void inject(DataSample<E> sample) {
            status = sample;
            broadcaster.broadcast(status);
        }

        protected final DataSample<E> getStatus() {
            return status;
        }
    }

    public class Sensor extends Device<Double> implements AnalogSensor {

        public Sensor(String address) {
            super(address);
        }

        @Override
        public JmxDescriptor getJmxDescriptor() {
            return new JmxDescriptor(
                    "dz",
                    getClass().getSimpleName(),
                    mqtt.host
                    + (mqtt.port == MQTT_DEFAULT_PORT ? "" : " port " + mqtt.port)
                    + " topic/pub " + mqtt.rootTopicPub
                    + ", topic/sub" + mqtt.rootTopicPub,
                    "sensor " + getAddress());
        }

        @Override
        public DataSample<Double> getSignal() {
            return getStatus();
        }
    }
}
