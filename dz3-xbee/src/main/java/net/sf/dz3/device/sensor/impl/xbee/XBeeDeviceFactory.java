package net.sf.dz3.device.sensor.impl.xbee;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.apache.logging.log4j.ThreadContext;

import com.rapplogic.xbee.api.ApiId;
import com.rapplogic.xbee.api.AtCommand;
import com.rapplogic.xbee.api.AtCommandResponse;
import com.rapplogic.xbee.api.PacketListener;
import com.rapplogic.xbee.api.RemoteAtRequest;
import com.rapplogic.xbee.api.XBee;
import com.rapplogic.xbee.api.XBeeAddress64;
import com.rapplogic.xbee.api.XBeeException;
import com.rapplogic.xbee.api.XBeeResponse;
import com.rapplogic.xbee.api.XBeeTimeoutException;
import com.rapplogic.xbee.api.zigbee.ZBNodeDiscover;
import com.rapplogic.xbee.api.zigbee.ZNetRxIoSampleResponse;
import com.rapplogic.xbee.util.ByteUtils;

import net.sf.dz3.device.factory.AbstractDeviceFactory;
import net.sf.dz3.device.factory.SingleSwitchProxy;
import net.sf.dz3.device.sensor.AnalogSensor;
import net.sf.dz3.device.sensor.DeviceContainer;
import net.sf.dz3.device.sensor.SensorType;
import net.sf.dz3.device.sensor.Switch;
import net.sf.dz3.device.sensor.impl.ContainerMap;
import net.sf.dz3.device.sensor.impl.StringChannelAddress;
import net.sf.dz3.instrumentation.Marker;
import com.homeclimatecontrol.jukebox.jmx.JmxAttribute;
import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;

/**
 * Factory for sensors and actuators implemented with XBee modules.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2009-2020
 */
public class XBeeDeviceFactory extends AbstractDeviceFactory<XBeeDeviceContainer> {

    private XBee coordinator = new XBee();
    private Listener listener = new Listener();

    private final String port;
    private final int baud;

    /**
     * Contains timestamps for sensor readings.
     */
    private final Map<String, Long> lastSeen = new TreeMap<>();

    /**
     * How open to perform {@link #browse(XBee)}.
     *
     * Default is one minute.
     *
     *  @see #execute()
     *  @see #browse(XBee)
     */
    private long rescanDelayMillis = 60000;

    /**
     * How old the sensor data has to be when it is considered stale.
     *
     * Default is one minute.
     *
     * @see #execute()
     * @see #ex
     */
    private long staleAgeMillis = 60000;

    /**
     * Create an instance using a given port.
     *
     * @param port Serial port XBee adapter is connected to.
     */
    public XBeeDeviceFactory(String port) {

        this(port, 9600);
    }

    /**
     * Create an instance using a given port at a given speed.
     *
     * Using this constructor is discouraged because default XBee baud rate is 9600,
     * and it is more than sufficient for DZ purposes.
     *
     * @param port Serial port XBee adapter is connected to.
     * @param baud Port speed.
     */
    public XBeeDeviceFactory(String port, int baud) {

        // No sanity checking, it'll blow up in startup()

        this.port = port;
        this.baud = baud;
    }

    @Override
    public Switch getSwitch(String compositeAddress) {

        ThreadContext.push("getSwitch");

        try {

            StringChannelAddress switchAddress = new StringChannelAddress(compositeAddress);
            String deviceAddress = Parser.render4x4(switchAddress.hardwareAddress);

            // Since there's no one to one correspondence between the XBee device container
            // and the single channel switch object, we have to create a proxy in any case
            // and let it take care of everything

            SwitchChannelSplitter proxy = address2proxy.get(deviceAddress);

            if (proxy == null) {

                proxy = new SwitchChannelSplitter();

                address2proxy.put(deviceAddress, proxy);
            }

            return proxy.getSwitch(switchAddress);

        } finally {
            ThreadContext.pop();
        }
    }

    @Override
    protected void startup() throws Throwable {

        ThreadContext.push("startup");

        try {

            coordinator.open(port, baud);

            logger.info("Opened {} @{} baud", port, baud);

            // Just in case, let's set AP=2 (xbee-api is unable to deal with API=1,
            // doesn't enforce AP=2 and causes subtle problems trying to communicate
            // through a device working with AP=1

            AP2(coordinator);

            // Connect the listener so we're not deaf

            coordinator.addPacketListener(listener);

            // Now let's see who's around
            browse(coordinator);

        } catch (Throwable t) {

            throw new IllegalStateException("Failed to initialize XBee", t);

        } finally {

            ThreadContext.pop();

            // VT: NOTE: Whereas NDC#remove() needed to be called here to prevent resource leaks with Log4j,
            // Log4j2 doesn't have it. Let's for now assume that ThreadContext#clearStack() takes care of it,
            // but let's also keep an eye on leaks and investigate if this is sufficient.

            ThreadContext.clearStack();
        }
    }

    @Override
    protected void execute() throws Throwable {

        ThreadContext.push("execute");

        try {

            while (isEnabled()) {

                logger.debug("Sleeping for {}ms", rescanDelayMillis);
                Thread.sleep(rescanDelayMillis);

                browse(coordinator);
                checkStale();
            }

        } finally {
            ThreadContext.pop();
        }
    }

    @Override
    protected void shutdown() throws Throwable {

        coordinator.close();
    }

    @JmxAttribute(description = "Network rescan period, milliseconds")
    public long getRescanDelayMillis() {

        return rescanDelayMillis;
    }

    public void setRescanDelayMillis(long rescanDelayMillis) {

        if (rescanDelayMillis < 30000) {

            throw new IllegalArgumentException("Unreasonably short rescan interval given ("
                    + rescanDelayMillis + "ms), minimum accepted is 30000");
        }

        if (rescanDelayMillis > staleAgeMillis) {

            throw new IllegalArgumentException("Rescan interval given ("
                    + rescanDelayMillis + "ms) is more than stale age (" + staleAgeMillis + "ms), don't be silly");
        }

        this.rescanDelayMillis = rescanDelayMillis;
    }

    @JmxAttribute(description = "Network rescan period, milliseconds")
    public long getStaleAgeMillis() {

        return staleAgeMillis;
    }

    public void setStaleAgeMillis(long staleAgeMillis) {

        if (rescanDelayMillis < 60000) {

            throw new IllegalArgumentException("Unreasonably short stale age given ("
                    + staleAgeMillis + "ms), minimum accepted is 60000");
        }

        this.staleAgeMillis = staleAgeMillis;
    }

    private void AP2(XBee target) throws XBeeException, IOException {

        XBeeResponse rsp = target.sendSynchronous(new AtCommand("AP", 2), XBeeConstants.TIMEOUT_AP_MILLIS);

        if (rsp.isError()) {
            throw new IOException("Can't set AP=2, response: " + rsp);
        }
    }

    /**
     * Initiate XBee network browse.
     *
     * @param target XBee device instance.
     *
     * @throws XBeeException
     */
    private void browse(XBee target) throws XBeeException {

        ThreadContext.push("browse");
        Marker m = new Marker("browse");

        try {

            // get the Node discovery timeout
            AtCommandResponse nodeTimeout = (AtCommandResponse) target.sendSynchronous(new AtCommand("NT"), XBeeConstants.TIMEOUT_NT_MILLIS);

            // default is 6 seconds
            long nodeDiscoveryTimeout = (ByteUtils.convertMultiByteToInt(nodeTimeout.getValue())) * 100L;

            logger.debug("Node discovery timeout is {} milliseconds", nodeDiscoveryTimeout);

            logger.debug("Sending node discover command");
            target.sendAsynchronous(new AtCommand("ND"));

            Thread.sleep(nodeDiscoveryTimeout);

            // The response will be obtained asynchronously by the Listener

            listFound();

        } catch (XBeeTimeoutException ex) {

            logger.error("Load too high or timeout too short?", ex);

        } catch (InterruptedException ex) {

            logger.error("Huh? Interrupted?", ex);

        } finally {

            m.close();
            ThreadContext.pop();
        }
    }

    private void listFound() {

        synchronized (address2dcGlobal) {

            StringBuilder sb = new StringBuilder();

            for (Iterator<String> i = address2dcGlobal.iterator(); i.hasNext(); ) {

                sb.append(i.next());

                if (i.hasNext()) {
                    sb.append(" ");
                }
            }

            logger.info("{} devices found: {}", address2dcGlobal.size(), sb);

            for (Iterator<String> i = address2dcGlobal.iterator(); i.hasNext(); ) {
                logger.info("found: {}", i.next());
            }
        }
    }

    /**
     * Create a generic prototype for the device found.
     *
     * All XBee devices are equal and can be used for any purpose (depending on
     * how they are programmed), so there is not a specialized container (like
     * there is in 1-Wire factory).
     *
     * @param nd Node Discover command response to create the prototype for.
     */
    private void createPrototype(ZBNodeDiscover nd) {

        ThreadContext.push("createProxy");

        try {

            logger.debug("Raw response: {}", nd);

            XBeeDeviceContainer proxy = new XBeeDeviceContainer(this, nd.getNodeAddress64());

            synchronized (address2dcGlobal) {
                address2dcGlobal.add(proxy);
            }

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Look for sensors that are not sending data anymore.
     *
     * Check if the sensor data is fresh enough and if it is not,
     * broadcast a failure notification.
     */
    private void checkStale() {

        ThreadContext.push("checkStale");

        try {

            long now = System.currentTimeMillis();

            for (Iterator<Entry<String, Long>> i = lastSeen.entrySet().iterator(); i.hasNext(); ) {

                Entry<String, Long> entry = i.next();
                String address = entry.getKey();
                long age = now - entry.getValue();

                if (age > staleAgeMillis) {

                    logger.warn("Stale sensor: {}", address);

                    XBeeDeviceContainer prototype = resolve(address);

                    if (prototype == null) {

                        logger.error("No prototype found for {}???", address);
                        continue;
                    }

                    prototype.broadcastFailure(new IOException(address
                            + " hasn't been seen for over " + staleAgeMillis + "ms (since"
                            + new Date(lastSeen.get(address))));
                }
            }

        } finally {
            ThreadContext.pop();
        }

    }

    /**
     * Resolve the instance of a device for the given hardware address.
     *
     * @param deviceAddress Hardware (not composite) address as string.
     * @return Container for the given address, or {@code null} if none found.
     */
    private XBeeDeviceContainer resolve(String deviceAddress) {

        ThreadContext.push("resolve");

        try {

            Set<DeviceContainer> deviceSet = address2dcGlobal.get(deviceAddress);

            // With XBee, it is safe to assume that there is one device in the set - the prototype

            // Unless there's none at all

            if (deviceSet == null) {

                // This will happen if the device was not present at browse() yet,
                // but managed to wake up and issue a sample since.
                //
                // This will correct itself at next browse().

                logger.warn("No devices for {} (first sample arrived before network browse?)", deviceAddress);
                return null;
            }

            // But let's check anyway

            if (deviceSet.size() != 1) {
                throw new IllegalStateException("Unexpected device set size (must be 1): " + deviceSet);
            }

            return (XBeeDeviceContainer) deviceSet.toArray()[0];

        } finally {
            ThreadContext.pop();
        }
    }

    public void broadcastIoSample(ZNetRxIoSampleResponse packet) {

        ThreadContext.push("broadcastIoSample");

        try {

            XBeeAddress64 xbeeAddress = packet.getRemoteAddress64();
            String deviceAddress = Parser.render4x4(xbeeAddress);

            XBeeDeviceContainer prototype = resolve(deviceAddress);

            // Let's proceed even if the prototype is null,
            // to record lastSeen and log the sample

            logger.debug("prototype: {}", prototype);

            int buffer[] = packet.getRawPacketBytes();
            int[] sampleBytes = new int[buffer.length - 14];

            for (int offset = 0; offset < buffer.length - 15; offset++) {

                int value = buffer[offset + 15];
                sampleBytes[offset] = value;
            }

            IoSample sample = new IoSample(sampleBytes, xbeeAddress, logger);

            logger.debug("sample: {}", sample);

            if (prototype != null) {
                prototype.broadcastIoSample(sample);
            }

            lastSeen.put(deviceAddress, System.currentTimeMillis());

        } finally {
            ThreadContext.pop();
        }
    }

    public XBeeResponse sendSynchronous(RemoteAtRequest request, int timeout) throws XBeeException {

        return coordinator.sendSynchronous(request, timeout);
    }

    @Override
    protected AnalogSensor createSensorProxy(String address, SensorType type) {

        // Short poll interval is OK - if the device isn't present, no big deal,
        // when it becomes available, the TemperatureProxy will take care of that anyway
        SensorProxy proxy = new XBeeSensorProxy(address, 1000, type);

        // If it doesn't start, help us God
        proxy.start();

        return proxy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JmxDescriptor getJmxDescriptor() {

        return new JmxDescriptor(
                "dz",
                getClass().getSimpleName(),
                port,
                "XBee device factory at " + baud + " baud on " + port);
    }

    private class XBeeSensorProxy extends SensorProxy {

        public XBeeSensorProxy(String address, int pollIntervalMillis, SensorType type) {

            super(address, pollIntervalMillis, type);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public JmxDescriptor getJmxDescriptor() {

            String rawAddress = getAddress();

            StringBuilder sb = new StringBuilder();

            for (StringTokenizer st = new StringTokenizer(rawAddress, ":"); st.hasMoreTokens(); ) {

                sb.append(st.nextToken());

                if (st.hasMoreTokens()) {

                    sb.append(" channel ");
                }
            }

            String address = type.type + sb.toString();

            logger.debug("JMX address for {} is '{}'", rawAddress, address);

            return new JmxDescriptor(
                    "dz",
                    getClass().getSimpleName(),
                    address,
                    "XBee " + type.description + " sensor , adress " + rawAddress);
        }

        @Override
        public String toString() {

            StringBuilder sb = new StringBuilder();

            sb.append(getClass().getSimpleName()).append("(").append(type.type).append(getAddress()).append(")");
            return sb.toString();
        }
    }

    private static class XBeeSingleSwitchProxy extends SingleSwitchProxy<XBeeSwitch> {

        public XBeeSingleSwitchProxy(ContainerMap address2dcGlobal, StringChannelAddress address) {

            super(address2dcGlobal, address);
        }

        @Override
        public synchronized boolean getState() throws IOException {

            ThreadContext.push("getState");

            try {

                XBeeSwitch xbeeSwitch = getContainer(address.toString());

                if (xbeeSwitch == null) {
                    throw new IOException("No container found for " + address + ", assuming not present");
                }

                return xbeeSwitch.getState();

            } finally {
                ThreadContext.pop();
            }
        }

        @Override
        public synchronized void setState(boolean state) throws IOException {

            ThreadContext.push("setState");

            try {

                XBeeSwitch xbeeSwitch = getContainer(address.toString());

                if (xbeeSwitch == null) {
                    throw new IOException("No container found for " + address + ", assuming not present");
                }

                xbeeSwitch.setState(state);

            } finally {
                ThreadContext.pop();
            }
        }

        @Override
        public JmxDescriptor getJmxDescriptor() {

            return new JmxDescriptor(
                    "dz",
                    getClass().getSimpleName(),
                    Integer.toHexString(hashCode()) + "#" + getAddress(),
                    "XBee single switch proxy");
        }
    }

    @Override
    protected Switch createSingleSwitchProxy(ContainerMap address2dcGlobal, StringChannelAddress switchAddress) {

      return new XBeeSingleSwitchProxy(address2dcGlobal, switchAddress);
    }

    private class Listener implements PacketListener {

        @Override
        public void processResponse(XBeeResponse packet) {

            ThreadContext.push("processResponse");

            try {

                logger.debug("packet: {}", packet);

                ApiId apiId = packet.getApiId();

                switch (apiId) {

                case AT_RESPONSE:

                    AtCommandResponse atCommandResponse = (AtCommandResponse) packet;
                    String command = atCommandResponse.getCommand();

                    if ("ND".equals(command)) {

                        createPrototype(ZBNodeDiscover.parse(atCommandResponse));

                    } else if ("NT".equals(command)) {

                        // No big deal, browse() initiated it and handled it

                    } else {

                        logger.warn("Unexpected response received (command: {})", command);
                    }

                    break;

                case ZNET_IO_SAMPLE_RESPONSE:

                    broadcastIoSample((ZNetRxIoSampleResponse) packet);
                    break;

                default:

                    // Have no idea what this is, don't care
                    break;
                }

            } catch (Throwable t) {

                logger.error("Oops", t);
            } finally {
                ThreadContext.pop();
            }

        }
    }
}
