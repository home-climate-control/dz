package net.sf.dz3.view.mqtt.v1;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import com.homeclimatecontrol.jukebox.datastream.logger.impl.DataBroadcaster;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSource;
import com.homeclimatecontrol.jukebox.jmx.JmxAware;
import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;

/**
 * MQTT device factory capable of working with <a href="https://esphome.io/">ESPHome</a> devices.
 *
 * For now, a quick and dirty copypaste derivation of {@link MqttDeviceFactory}.
 * Common details to be pushed into an abstract superclass as soon as the
 * implementation is usable.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class ESPHomeDeviceFactory implements DeviceFactory2020, AutoCloseable, JmxAware {

    protected final Logger logger = LogManager.getLogger(getClass());

    private final MqttContext mqtt;
    private final Watchdog watchdog;
    private final Thread watchdogThread;
    private final CountDownLatch stopGate = new CountDownLatch(1);
    private Set<String> seenAlienTopic = new HashSet<>();

    static final long POLL_INTERVAL = 10000L;
    static final long STALE_AGE = POLL_INTERVAL * 5;

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
     */
    public ESPHomeDeviceFactory(
            String mqttBrokerHost,
            String mqttRootTopicPub, String mqttRootTopicSub) throws MqttException {

        this(mqttBrokerHost, MqttContext.DEFAULT_PORT, null, null, mqttRootTopicPub, mqttRootTopicSub);
    }

    /**
     * Unauthenticated constructor with a custom port.
     *
     * @param mqttBrokerHost Host to connect to.
     * @param mqttBrokerPort Port to connect to.
     * @param mqttRootTopicPub Root topic to publish to.
     * @param mqttRootTopicSub Root topic to subscribe to.
     */
    public ESPHomeDeviceFactory(
            String mqttBrokerHost, int mqttBrokerPort,
            String mqttRootTopicPub, String mqttRootTopicSub) throws MqttException {

        this(mqttBrokerHost, mqttBrokerPort, null, null, mqttRootTopicPub, mqttRootTopicSub);
    }

    /**
     * Authenticated constructor with a default port.
     *
     * @param mqttBrokerHost Host to connect to.
     * @param mqttBrokerUsername MQTT broker username.
     * @param mqttBrokerPassword MQTT broker password.
     * @param mqttRootTopicPub Root topic to publish to.
     * @param mqttRootTopicSub Root topic to subscribe to.
     */
    public ESPHomeDeviceFactory(
            String mqttBrokerHost,
            String mqttBrokerUsername, String mqttBrokerPassword,
            String mqttRootTopicPub, String mqttRootTopicSub) throws MqttException {

        this(mqttBrokerHost, MqttContext.DEFAULT_PORT, mqttBrokerUsername, mqttBrokerPassword, mqttRootTopicPub, mqttRootTopicSub);
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
     */
    public ESPHomeDeviceFactory(
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
                + (mqtt.port == MqttContext.DEFAULT_PORT ? "" : " port " + mqtt.port)
                + " topic/pub " + mqtt.rootTopicPub
                + " topic/sub " + mqtt.rootTopicSub,
                "ESPHomeDeviceFactory v1");
    }


    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void refresh() {
        ThreadContext.push("refresh");
        try {

            logger.debug("heartbeat");

            mqtt.reconnect();

            // VT: FIXME: use java.time.Clock
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

    void process(String topic, byte[] source) {
        ThreadContext.push("process");
        try {

            String args[] = parseTopic(topic);

            if (args == null) {
                return;
            }

            // VT: FIXME: use java.time.Clock
            processSensorInput(args[1], new BigDecimal(new String(source)), System.currentTimeMillis(), args[1], args[0]);

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * ESPHome MQTT topic matching pattern.
     *
     * It is possible to optimize it to use numbered groups, but is it worth it?
     */
    private final Pattern p = Pattern.compile("(?<deviceId>.*)/sensor/(?<sensorName>.*)/state");

    /**
     * Parse the device ID and the sensor name out of the topic.
     *
     * @param source Topic as a string.
     *
     * @return An array where the first element is the topic prefix (interpreted as device ID)
     * and the second is the sensor name.
     */
    @java.lang.SuppressWarnings({"squid:S1168"})
    private String[] parseTopic(String source) {

        // That "not a sensor" debug statement below will drive the disk into the ground, better avoid it if possible
        if (seenAlienTopic.contains(source)) {

            // Trace is rarely enabled, no big deal
            logger.trace("seen '{}' already, not matching", source);

            // VT: NOTE: squid:S1168 I'm not going to waste memory to indicate a "skip" condition
            return null;
        }

        // The typical ESPHome topic will look like this:
        //
        // ${ESPHome-topic-prefix}/sensor/${ESPHome-sensor-name}/state

        Matcher m = p.matcher(source);
        m.find();

        if (!m.matches()) {

            logger.debug("{}: not a sensor (this message will repeat once per run)", source);

            // We don't want to see this message again
            seenAlienTopic.add(source);

            // VT: NOTE: squid:S1168 I'm not going to waste memory to indicate a "skip" condition
            return null;
        }

        return new String[] { m.group("deviceId"), m.group("sensorName")};
    }

    void processSensorInput(
            String name,
            BigDecimal signal,
            long timestamp,
            String signature,
            String deviceId) {

        ThreadContext.push("processSensorInput");
        try {

            Device<?> d = deviceMap.get(name);

            if (d == null) {
                logger.debug("not ours: {}", name);
                return;
            }

            Sensor s = (Sensor) d;
            double v = signal.doubleValue();
            s.inject(new DataSample<Double>(timestamp, s.getAddress(), deviceId + "/" + signature, v, null));

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

                // VT: NOTE: When refactoring: MqttDeviceFactory just cares about content, we
                // care about the topic as well, need to tweak the process() method signature

                process(topic, message.getPayload());

            } catch (Throwable t) {

                // VT: NOTE: According to the docs, throwing an exception here will shut down the client - can't afford that,
                // so we'll just complain loudly

                ThreadContext.push("error");

                logger.error("MQTT message caused an exception");
                logger.error("topic: {}", topic);
                logger.error("payload: {}", new String(message.getPayload()));
                logger.error("trace", t);

                ThreadContext.pop();

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

    private abstract class Device<E> implements DataSource<E>, Addressable, JmxAware {

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
                    + (mqtt.port == MqttContext.DEFAULT_PORT ? "" : " port " + mqtt.port)
                    + " topic/pub " + mqtt.rootTopicPub
                    + " topic/sub " + mqtt.rootTopicSub,
                    "sensor " + getAddress());
        }

        @Override
        public DataSample<Double> getSignal() {
            return getStatus();
        }
    }
}
