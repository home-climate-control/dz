package net.sf.dz3.view.mqtt.v1;

import com.homeclimatecontrol.jukebox.datastream.logger.impl.DataBroadcaster;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSource;
import com.homeclimatecontrol.jukebox.jmx.JmxAware;
import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;
import net.sf.dz3.device.sensor.Addressable;
import net.sf.dz3.device.sensor.AnalogSensor;
import net.sf.dz3.device.sensor.DeviceFactory2020;
import net.sf.dz3.device.sensor.Switch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;

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
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
abstract public class AbstractMqttDeviceFactory implements DeviceFactory2020, AutoCloseable, JmxAware {

    protected final Logger logger = LogManager.getLogger(getClass());
    private final MqttContext mqtt;
    protected final Watchdog watchdog;
    private final Thread watchdogThread;
    private final CountDownLatch stopGate = new CountDownLatch(1);
    protected final Set<String> seenAlienTopic = new HashSet<>();

    static final long POLL_INTERVAL = 10000L;
    static final long STALE_AGE = POLL_INTERVAL * 5;

    /**
     * Data map.
     *
     * The key is the device address, the value is device itself.
     */
    protected final Map<String, Device<?>> deviceMap = new TreeMap<>();

    public AbstractMqttDeviceFactory(
            String mqttBrokerHost, int mqttBrokerPort,
            String mqttBrokerUsername, String mqttBrokerPassword,
            String mqttRootTopicPub, String mqttRootTopicSub) throws MqttException {

        this.mqtt = new MqttContext(
                mqttBrokerHost, mqttBrokerPort,
                mqttBrokerUsername, mqttBrokerPassword,
                mqttRootTopicPub, mqttRootTopicSub + "/#",
                createCallback());

        mqtt.start();

        // VT: NOTE: Clumsy, but we need access to both the thread (to stop it) and the runnable (to notify())
        watchdog = new Watchdog();
        watchdogThread = new Thread(new Watchdog());
        watchdogThread.start();
    }

    @Override
    public AnalogSensor getSensor(String address) {
        return new ESPHomeDeviceFactory.Sensor(address);
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
                getClass().getSimpleName() + " v1");
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


    @java.lang.SuppressWarnings({"squid:S2142","squid:S2189"})
    protected class Watchdog implements Runnable {

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

    protected abstract class Device<E> implements DataSource<E>, Addressable, JmxAware {

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

        @Override
        public int compareTo(Addressable o) {
            // Can't afford to collide with the wrapper
            return (getClass().getName() + getAddress()).compareTo((o.getClass().getName() + o.getAddress()));
        }
    }

    abstract protected MqttCallback createCallback();
}
