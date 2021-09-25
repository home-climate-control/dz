package net.sf.dz3.device.sensor.impl.onewire;

import com.dalsemi.onewire.OneWireAccessProvider;
import com.dalsemi.onewire.OneWireException;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.adapter.OneWireIOException;
import com.dalsemi.onewire.adapter.USerialAdapter;
import com.dalsemi.onewire.container.HumidityContainer;
import com.dalsemi.onewire.container.OneWireContainer;
import com.dalsemi.onewire.container.SwitchContainer;
import com.dalsemi.onewire.container.TemperatureContainer;
import com.dalsemi.onewire.utils.OWPath;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import com.homeclimatecontrol.jukebox.jmx.JmxAware;
import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;
import net.sf.dz3.device.factory.AbstractDeviceFactory;
import net.sf.dz3.device.factory.DataMap;
import net.sf.dz3.device.factory.SingleSwitchProxy;
import net.sf.dz3.device.sensor.Addressable;
import net.sf.dz3.device.sensor.AnalogSensor;
import net.sf.dz3.device.sensor.DeviceContainer;
import net.sf.dz3.device.sensor.SensorType;
import net.sf.dz3.device.sensor.Switch;
import net.sf.dz3.device.sensor.impl.ContainerMap;
import net.sf.dz3.device.sensor.impl.StringChannelAddress;
import net.sf.dz3.instrumentation.Marker;
import org.apache.logging.log4j.ThreadContext;

import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * 1-Wire device factory.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class OwapiDeviceFactory extends AbstractDeviceFactory<OneWireDeviceContainer> implements OneWireNetworkEventListener {

    private static final String DEVICE_DS2409 = "DS2409";

    /**
     * Adapter port.
     *
     * This value is injected via constructor. If the port is bad, the device factory
     * will fail to {@link #start()}.
     */
    private final String adapterPort;

    /**
     * Adapter speed.
     *
     * This value is injected via constructor. If the value given is bad, it will be
     * defaulted to {@link DSPortAdapter.Speed#REGULAR}.
     */
    private final DSPortAdapter.Speed adapterSpeed;

    /**
     * Mapping from the adapter speed value to speed name.
     */
    private final Map<DSPortAdapter.Speed, String> speed2name = new TreeMap<>();

    /**
     * 1-Wire adapter.
     *
     * Initialized in {@link #startup()}.
     */
    private DSPortAdapter adapter = null;

    /**
     * Device map.
     * <p>
     * The key is the device path, the value is a sorted map where the key is
     * the hardware address, and the value is the device container. Such a
     * complication is required to optimize the access by opening the minimal
     * number of paths and eliminating redundancy.
     *
     * @see AbstractDeviceFactory#address2dcGlobal
     */
    Map<OWPath, ContainerMap> path2device = new TreeMap<>();

    /**
     * The network monitor.
     */
    private OneWireNetworkMonitor monitor;

    /**
     * Low-level state map. The key is the device address, the value is last
     * known state obtained using {@code readDevice()}.
     */
    protected Map<String, byte[]> stateMap = new TreeMap<>();

    /**
     * Create an instance.
     *
     * @param port Port to use.
     * @param speed Speed to use (choices are "regular", "flex", "overdrive", "hyperdrive".
     */
    public OwapiDeviceFactory(String port, String speed) {

        ThreadContext.push("DeviceFactory");

        try {

            if (port == null || "".equals(port)) {
                throw new IllegalArgumentException("port can't be null or empty");
            }

            adapterPort = port;

            logger.info("Port:  {}", adapterPort);

            var name2speed = new TreeMap<String, DSPortAdapter.Speed>();

            name2speed.put("overdrive", DSPortAdapter.Speed.OVERDRIVE);
            name2speed.put("hyperdrive", DSPortAdapter.Speed.HYPERDRIVE);
            name2speed.put("flex", DSPortAdapter.Speed.FLEX);
            name2speed.put("regular", DSPortAdapter.Speed.REGULAR);

            speed2name.put(DSPortAdapter.Speed.OVERDRIVE, "overdrive");
            speed2name.put(DSPortAdapter.Speed.HYPERDRIVE, "hyperdrive");
            speed2name.put(DSPortAdapter.Speed.FLEX, "flex");
            speed2name.put(DSPortAdapter.Speed.REGULAR, "regular");

            var speedValue = name2speed.get(speed);

            if (speedValue == null) {
                logger.warn("Unknown speed '{}', defaulted to regular", speed);
            }

            adapterSpeed = speedValue == null ? DSPortAdapter.Speed.REGULAR : speedValue;

            logger.info("Speed: {}", speed2name.get(adapterSpeed));

        } finally {
            ThreadContext.pop();
        }

    }

    @Override
    protected AnalogSensor createSensorProxy(String address, SensorType type) {

        // Short poll interval is OK - if the device isn't present, no big deal,
        // when it becomes available, the TemperatureProxy will take care of that anyway
        var proxy = new OwapiSensorProxy(address, 1000, type, getMonitor());

        // If it doesn't start, help us God
        proxy.start();

        return proxy;
    }

    /**
     * Get an instance of a switch.
     *
     * @param compositeAddress 1-Wire address, followed by a colon, followed by channel number.
     *
     * @return An instance of a {@link Switch single channel switch}, unconditionally. In case when
     * the device with a given address is not present on a bus, no indication will be given
     * until the actual operation (accessor or mutator) is performed, which will result in
     * {@link IOException}.
     */
    @Override
    public synchronized Switch getSwitch(String compositeAddress) {

        ThreadContext.push("getSwitch");

        try {

            var switchAddress = new StringChannelAddress(compositeAddress);

            // Since there's no one to one correspondence between the 1-Wire device container
            // and the single channel switch object, we have to create a proxy in any case
            // and let it take care of everything

            var proxy = address2proxy.get(switchAddress.hardwareAddress);

            if (proxy == null) {

                proxy = new SwitchChannelSplitter();

                address2proxy.put(switchAddress.hardwareAddress, proxy);
            }

            return proxy.getSwitch(switchAddress);

        } finally {
            ThreadContext.pop();
        }
    }

    @SuppressWarnings("squid:S1181")
    @Override
    protected void startup() throws Throwable {

        ThreadContext.push("startup");

        try {

            boolean ok;
            try {

                adapter = new USerialAdapter();

                // VT: NOTE: Having succeeded at selecting the port doesn't necessarily mean that we'll be fine.
                // Serial based adapters don't seem to be accessed during selectPort(), and it's quite
                // possible to successfully select a port that doesn't correspond to an existing adapter.
                // Additional test is required to make sure we're OK.

                ok = adapter.selectPort(adapterPort);

            } catch (OneWireException ex) {
                logger.error("Failed to open port '{}'", adapterPort, ex);
                ok = false;
            }

            if (!ok) {

                throw new IllegalArgumentException("Port '" + adapterPort + "' unavailable, valid values: "
                        + DSPortAdapter.getPortNames() + "\n"
                        + "Things to check:\n"
                        + "    http://stackoverflow.com/questions/9628988/ubuntu-rxtx-does-not-recognize-usb-serial-device yet?");
            }

            try {

                // Now, *this* should take care of it...
                adapter.reset();

            } catch (OneWireIOException ex) {

                if ("Error communicating with adapter".equals(ex.getMessage())) {
                    throw new IOException("Port '" + adapterPort
                            + "' doesn't seem to have adapter connected, check others: " + DSPortAdapter.getPortNames(), ex);
                }
            }

            logger.info("Adapter class: {}", adapter.getClass().getName());
            logger.info("Adapter port:  {}", adapterPort);

            // VT: NOTE: It is not necessary to use the lock at this point
            // because the service is not yet started

            try {

                logger.info("Setting adapter speed to {}", speed2name.get(adapterSpeed));
                adapter.setSpeed(adapterSpeed);

            } catch (Throwable t) {

                // squid:S1181: No.
                logger.error("Failed to set adapter speed, cause:", t);
            }

            monitor = new OneWireNetworkMonitor(adapter, lock);

            synchronized (this) {

                // This is necessary to release getMonitor()
                notifyAll();
            }
            monitor.start();

            monitor.addListener(this);
            monitor.getSemUp().waitFor();

            logger.info("started");

        } finally {
            ThreadContext.pop();
            ThreadContext.clearStack();
        }
    }

    /**
     * Get the network monitor.
     *
     * The reason for existence of this method is to retrieve the monitor instance
     * to register it with JMX. Other than this, you never, ever have to do anything
     * with the network monitor, and this method shouldn't exist at all - except for
     * the memory leak that has to be tracked down and eliminated.
     *
     * @return The 1-Wire network monitor.
     */
    public synchronized OneWireNetworkMonitor getMonitor() {

        while (monitor == null) {

            logger.info("Waiting for the monitor to become available");

            try {

                wait(1000);

            } catch (InterruptedException ex) {
                logger.warn("Interrupted, ignored, waiting some more", ex);
                Thread.currentThread().interrupt();
            }
        }

        return monitor;
    }

    /**
     * Find all available ports and assign the adapter for the one we need,
     * if possible.
     *
     * @return Set of available port names as strings.
     */
    private Set<String> getPortsAvailable() {

        ThreadContext.push("getPortsAvailable");

        try {

            var portsAvailable = new TreeSet<String>();

            for (DSPortAdapter a : OneWireAccessProvider
                    .getAdapters()) {

                logger.debug("Adapter found: {}", a.getAdapterName());

                for (String portName : a.getPortNames()) {

                    logger.debug("Port found: {}", portName);

                    if (adapterPort.equals(portName)) {

                        adapter = a;

                        // Let's not break here, to collect all available port
                        // names
                    }

                    portsAvailable.add(portName);
                }
            }

            return portsAvailable;
        } finally {
            ThreadContext.pop();
        }
    }

    @Override
    protected void shutdown() throws Throwable {

        ThreadContext.push("shutdown");

        try {

            logger.info("Stopping monitor...");
            monitor.stop().waitFor();
            logger.info("Stopped");

        } finally {
            ThreadContext.pop();
            ThreadContext.clearStack();
        }
    }

    /**
     * Keep polling the device state until stopped.
     *
     * @exception Throwable if anything goes wrong.
     */
    @SuppressWarnings("squid:S1181")
    @Override
    protected final void execute() throws Throwable {

        ThreadContext.push("execute");

        try {

            while (isEnabled()) {

                try {

                    poll();

                } catch (ConcurrentModificationException ex) {

                    // No big deal

                    logger.debug("Arrival/departure during poll, ignored", ex);

                } catch (Throwable t) {

                    if (t instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }

                    // squid:S1181: No.
                    logger.error("Poll broken:", t);

                    // Possibly, the cause of this was a network departure.
                    // Let's refresh the device map

                    monitor.rescan().waitFor();
                }
            }

        } finally {
            ThreadContext.pop();
            ThreadContext.clearStack();
        }
    }

    private void poll() throws InterruptedException, OneWireException {

        ThreadContext.push("poll");
        var m = new Marker("poll");

        try {

            var localDataMap = new DataMap();

            if (path2device.isEmpty()) {

                logger.debug("No devices - forcing rescan");

                monitor.rescan().waitFor();

                return;
            }

            for (OWPath owPath : path2device.keySet()) {

                // The lock gets acquired and released only here
                processPath(owPath, localDataMap);

                // If someone else wants to work with devices, this point is where they get the lock during te poll
            }

            if (path2device.isEmpty()) {

                logger.warn("path2device is empty; do we need to wait?");

                Thread.sleep(1000);
            }

            localDataMap.transferTo(dataMap);
            logger.debug("Data map: {}", dataMap);

        } finally {

            m.close();
            ThreadContext.pop();
        }
    }

    @SuppressWarnings("squid:S1181")
    private void processPath(OWPath path, DataMap dataMap) throws OneWireException {

        ThreadContext.push("processPath");
        var m = new Marker("processPath");

        logger.debug("Processing {}", path);

        lock.writeLock().lock();

        try {

            m.checkpoint("got lock");

            path.open();

            m.checkpoint("open");

            ContainerMap address2dcForPath = path2device.get(path);

            if (address2dcForPath == null || address2dcForPath.isEmpty()) {

                logger.warn("No addresses present for  '{}'", path);
                return;
            }

            // VT: NOTE: To speed things up, it would make sense to
            // go a level down. Issue a temperature convert command for
            // all the temperature sensors on this branch, then read
            // them all. Don't forget to check if there are any
            // temperature sensors on the branch.

            for (var ai = address2dcForPath.iterator(); ai.hasNext();) {

                // To improve the shutdown time, let's check if
                // we're still enabled, otherwise we might be just
                // going, and going, and going...

                if (!isEnabled()) {

                    logger.info("Oops! Not enabled anymore...");
                    return;
                }

                var address = ai.next();
                var dcSet = address2dcForPath.get(address);

                processAddress(address, dcSet);
            }

        } catch (OneWireException ex) {

            logger.error("Can't process path '{}', 1-Wire exception rethrown", path);
            throw ex;

        } catch (Throwable t) {

            // squid:S1181: No.
            throw new OneWireException("Can't process path " + path, t);

        } finally {

            lock.writeLock().unlock();

            m.close();
            ThreadContext.pop();
        }
    }

    @SuppressWarnings({"squid:S1181", "deprecation"})
    private void processAddress(String address, Set<DeviceContainer> dcSet) throws OneWireException {

        ThreadContext.push("processAddress");

        try {

            if (dcSet == null) {

                logger.warn("No devices for {}???", address);
                return;
            }

            logger.debug("processing {}", address);

            for (DeviceContainer deviceContainer : dcSet) {

                OneWireDeviceContainer dc = (OneWireDeviceContainer) deviceContainer;
                var owc = dc.container;

                // VT: NOTE: The 1-Wire container is wrapped
                // into the device container. Therefore, it
                // is possible that we'll run into the same
                // 1-Wire container more than once,
                // therefore, we should be checking the
                // device container class, not the 1-Wire
                // container, to prevent redundant probes.

                try {

                    if (dc instanceof OneWireTemperatureContainer) {

                        TemperatureContainer tc = (TemperatureContainer) owc;

                        var temp = getTemperature(tc);

                        logger.debug("{}: {}°C", address, temp);

                        dataMap.put(address, Type.TEMPERATURE, temp);

                        stateChanged(dc, temp);

                    } else if (dc instanceof OneWireSwitchContainer) {

                        // Don't do it if the device is DS2409 - we'll get
                        // screwed up trying to restore the state.
                        // They are managed automatically when the path is
                        // selected anyway.

                        if (!DEVICE_DS2409.equals(owc.getName())) {
                            dataMap.put(address, Type.SWITCH, getState((SwitchContainer) owc));
                        }

                    } else if (dc instanceof OneWireHumidityContainer) {

                        HumidityContainer hc = (HumidityContainer) owc;

                        long hstart = System.currentTimeMillis();

                        byte[] state = hc.readDevice();

                        hc.doHumidityConvert(state);

                        double humidity = hc.getHumidity(state);

                        logger.debug("Humidity: {} (took {}ms to figure out)", humidity, (System.currentTimeMillis() - hstart));

                        dataMap.put(address, Type.HUMIDITY, humidity);

                        stateChanged(dc, humidity);
                    }

                } catch (OneWireException ex) {

                    stateChanged(dc, ex);

                    logger.error("Failed to read {}, 1-Wire exception rethrown", address);
                    throw ex;

                } catch (Throwable t) {

                    // squid:S1181: No.
                    stateChanged(dc, t);

                    throw new OneWireException("Failed to read " + address + ", cause:", t);
                }
            }

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Handle a network arrival.
     *
     * @param e Network arrival information.
     */
    @SuppressWarnings({"squid:S1181", "deprecation"})
    @Override
    public void networkArrival(OneWireNetworkEvent e) {

        // We know it's actually a different class...

        OwapiNetworkEvent e2 = (OwapiNetworkEvent) e;

        // It is assumed that the device that came with this event is not in
        // the device map. If it is, we've screwed up somewhere else...
        // Let's check it.

        var owc = e2.getDeviceContainer();
        var address = owc.getAddressAsString();

        Set<DeviceContainer> dcSet = address2dcGlobal.get(address);

        if (dcSet != null) {

            for (DeviceContainer oldContainer : dcSet) {

                if (oldContainer != null) {

                    logger.warn("Arrival notification for device already present: {}", e);
                    logger.warn("Duplicate device is: {}", oldContainer);
                }
            }
        }

        // Let's set the resolution before registering the device with the
        // device map so the poll doesn't interfere, and the device state
        // gets cached

        if (owc instanceof TemperatureContainer) {

            for (var retry = 0; retry < 5; retry++) {

                try {

                    setHiRes((TemperatureContainer) owc, e2.path);
                    break;

                } catch (Throwable t) {

                    // squid:S1181: No.
                    logger.warn("Failed to setHiRes on {}, trying again ({})", address, retry);
                }
            }

        } else if (owc instanceof SwitchContainer) {

            // Restore the device state. DS2409 are not included in the map
            // anyway.

            SwitchState ss = (SwitchState) dataMap.get(address, Type.SWITCH);

            if (ss != null) {

                // Yup, we've seen this device before

                SwitchContainer sc = (SwitchContainer) owc;

                try {

                    lock.writeLock().lock();

                    var state = sc.readDevice();

                    sc.setLatchState(0, ss.state[0], ss.smart, state);
                    sc.setLatchState(1, ss.state[1], ss.smart, state);

                    sc.writeDevice(state);

                    logger.warn("Restored state for {}: {}", address, ss);

                } catch (OneWireException ex) {

                    logger.error("Failed to restore switch state ({}), cause:", address, ex);

                } finally {

                    lock.writeLock().unlock();
                }
            }
        }

        ContainerMap address2dcForPath = path2device.get(e2.path);

        if (address2dcForPath == null) {

            address2dcForPath = new ContainerMap();

            path2device.put(e2.path, address2dcForPath);
        }

        Set<OneWireDeviceContainer> newDcSet = createContainer(owc);

        for (DeviceContainer dc : newDcSet) {

            logger.debug("Created container: {} ({})", dc, dc.getType());

            address2dcForPath.add(dc);
            address2dcGlobal.add(dc);
        }
    }

    /**
     * Try to set the highest possible resolution available from the temperature
     * container.
     *
     * @param tc Temperature container to set the resolution of.
     * @param path Path to reach the container.
     */
    @SuppressWarnings({"squid:S1181", "deprecation"})
    private void setHiRes(final TemperatureContainer tc, final OWPath path) {

        try {

            lock.writeLock().lock();

            path.open();

            var state = tc.readDevice();

            if (tc.hasSelectableTemperatureResolution()) {

                var resolution = tc.getTemperatureResolutions();
                var sb = new StringBuilder();

                for (double v : resolution) {
                    sb.append(v).append(" ");
                }

                logger.debug("Temperature resolutions available: {}", sb);

                tc.setTemperatureResolution(resolution[resolution.length - 1], state);
            }

            tc.writeDevice(state);

            stateMap.put(((OneWireContainer) tc).getAddressAsString(), state);

        } catch (Throwable t) {

            // squid:S1181: No.
            logger.warn("Failed to set high resolution on {}, cause:", ((OneWireContainer) tc).getAddressAsString(), t);

        } finally {

            lock.writeLock().unlock();
        }
    }

    /**
     * Create DZ containers for 1-Wire container.
     *
     * @param owc 1-Wire device container.
     * @return Set of DZ device containers created for a given 1-Wire device
     * container.
     */
    private Set<OneWireDeviceContainer> createContainer(final OneWireContainer owc) {

        var result = new TreeSet<OneWireDeviceContainer>();

        // Let's see what kind of a device we have...

        if (owc instanceof HumidityContainer) {
            result.add(new OneWireHumidityContainer(owc));
        }

        if (owc instanceof TemperatureContainer) {
            result.add(new OneWireTemperatureContainer(owc));
        }

        // VT: NOTE: This means we can't use DS2409 for actuators, only branches
        if ((owc instanceof SwitchContainer) && !(DEVICE_DS2409.equals(owc.getName()))) {

            result.add(new OneWireSwitchContainer(this, owc));
        }

        if (result.isEmpty()) {

            // This is too deep down the stack, and we can't afford to throw
            // an exception. Let's create a generic container and let the
            // callers figure the rest out.

            // VT: NOTE: This is a kludge, but it worked since 2000
            if (DEVICE_DS2409.equals(owc.getName())) {
                logger.info("Skipping (can only be a coupler) {}, generic container created", owc);
            } else {
                logger.info("createContainer(): don't know how to handle {}, generic container created", owc);
            }

            result.add(new OneWireDeviceContainer(owc));
        }

        return result;
    }

    /**
     * Handle a network departure.
     *
     * @param e Event to handle.
     */
    @Override
    public void networkDeparture(OneWireNetworkEvent e) {

        ThreadContext.push("networkDeparture/event");

        try {

            // We know it's actually a different class...

            var e2 = (OwapiNetworkEvent) e;

            // The only valid element in the event is the address

            Set<DeviceContainer> oldContainers = address2dcGlobal.remove(e.address);

            if (oldContainers == null) {
                logger.warn("Departure notification for device that is not present: {}", e.address);
            }

            // If the path is known, we don't have to iterate through all of
            // them

            var removed = false;

            if (e2.path != null) {

                logger.debug("Departure on known path: {}", e2.path);

                ContainerMap address2dcForPath = path2device.get(e2.path);

                if (address2dcForPath == null) {

                    logger.warn("networkDeparture({}): No devices for path {}", e, e2.path);

                    // dumpMap(path2device, "path2device");

                    removed = networkDeparture(e2.address);

                } else {

                    removed = networkDeparture(address2dcForPath, e2.address);
                }

                if (address2dcForPath != null && address2dcForPath.isEmpty()) {

                    logger.info("Empty path {}, removed", e2.path);

                    path2device.remove(e2.path);
                }

            } else {

                // All right, we don't have the path... Why?

                removed = networkDeparture(e2.address);
            }

            if (!removed) {

                // The device wasn't found???

                logger.warn("Got the departure notification before arrival notification for {}", e.address);
            }

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Handle a network departure for a known path.
     *
     * @param address2dcForPath Address to device mapping for the path the
     * device is supposed to be.
     * @param address Device address to handle departure of.
     * @return true if device has been successfully unmapped, false otherwise.
     */
    private boolean networkDeparture(final ContainerMap address2dcForPath, final String address) {

        ThreadContext.push("networkDeparture/device");

        try {

            if (address2dcForPath == null) {

                // This should have been checked up the stack

                throw new IllegalArgumentException("Null map for " + address);
            }

            Set<DeviceContainer> dcSet = address2dcForPath.get(address);

            if (dcSet == null || dcSet.isEmpty()) {

                // Nope, this is not our device

                return false;
            }

            // O! This is the one

            // dumpMap(address2dcForPath, "address2dcForPath before");

            Set<DeviceContainer> removed = address2dcForPath.remove(address);

            for (DeviceContainer container : removed) {
                logger.debug("Removed: {} (#{})", container, Integer.toHexString(container.hashCode()));
            }

            // dumpMap(address2dcForPath, "address2dcForPath after");

            // We don't need the device state either, lest it gets cached and
            // interferes with the subsequent reads

            stateMap.remove(address);

            return true;

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Handle a network departure for unknown path.
     *
     * @param address Device addres to handle departure of.
     * @return true if device has been sucessfully unmapped, false otherwise.
     */
    private boolean networkDeparture(final String address) {

        var removed = false;

        logger.info("Departure on unknown path");

        for (Iterator<OWPath> pi = path2device.keySet().iterator(); pi.hasNext();) {

            OWPath path = pi.next();
            ContainerMap address2dcForPath = path2device.get(path);

            if (address2dcForPath == null) {

                logger.warn("networkDeparture({}): No devices for path {}", address, path);

                // dumpMap(path2device, "path2device");

                continue;
            }

            removed = networkDeparture(address2dcForPath, address);

            /*
             * if (removed) { dumpMap(address2dcForPath, "address2dcForPath
             * AFTER"); }
             */

            if (address2dcForPath.isEmpty()) {

                // There are no more devices on this path, so there's no
                // need to open it at all

                logger.info("Path doesn't contain any devices, removed: {}", path);
                pi.remove();
            }

            if (removed) {

                break;
            }
        }

        return removed;
    }

    @Override
    public void networkFault(OneWireNetworkEvent e, String message) {

        // VT: NOTE: This is pretty bad, most probably a 1-Wire network short
        // circuit. But this is rare enough to be worrying about it at this stage.

        throw new UnsupportedOperationException("Not Implemented: handling '" + message + "', event: " + e);
    }

    /**
     * Get the temperature container reading.
     *
     * @param tc Temperature container to get the reading from.
     * @exception OneWireException if there was a problem talking to 1-Wire&reg;
     * device.
     * @return Current temperature.
     * @throws OneWireIOException If there was a problem with 1-Wire subsystem.
     */
    @SuppressWarnings("deprecation")
    final double getTemperature(final TemperatureContainer tc) throws OneWireException {

        ThreadContext.push("getTemperature");
        var m = new Marker("getTemperature");

        try {

            // get the current resolution and other settings of the device

            var address = ((OneWireContainer) tc).getAddressAsString();
            double lastTemp;

            // VT: NOTE: What if the state is not available yet?
            // Theoretically, it should be 'cause setHiRes should have been
            // called, but this has to be verified

            byte[] state = stateMap.get(address);

            if (state == null) {

                logger.warn("device state is not available yet, possibly setHiRes failed");

                state = tc.readDevice();
            }

            m.checkpoint("readDevice/0");

            // perform a temperature conversion

            tc.doTemperatureConvert(state);

            m.checkpoint("doTemperatureConvert");

            // read the result of the conversion

            state = tc.readDevice();

            m.checkpoint("readDevice/1");

            // extract the result out of state
            lastTemp = tc.getTemperature(state);

            if (lastTemp == 85.0) {

                // Known bug, ignore

                throw new IllegalStateException("Temp read is 85C, ignored");
            }

            stateMap.put(address, state);

            return lastTemp;

        } finally {

            m.close();
            ThreadContext.pop();
        }
    }

    /**
     * Get the switch container state.
     *
     * @param sc Switch container to get the state of.
     * @return The switch state object.
     * @exception OneWireException if there was a problem with 1-Wire API.
     */
    @SuppressWarnings("deprecation")
    private SwitchState getState(final SwitchContainer sc) throws OneWireException {

        // We don't have to get the lock here, because we're in the lock
        // protected context already

        var ss = new SwitchState();
        var state = sc.readDevice();

        ss.smart = sc.hasSmartOn();
        ss.state[0] = sc.getLatchState(0, state);
        ss.state[1] = sc.getLatchState(1, state);

        return ss;
    }

    /**
     * Broadcast the signal notification.
     *
     * @param dc Device container for the sensor whose state has changed.
     * @param value Current sensor reading.
     */
    private void stateChanged(final DeviceContainer dc, final double value) {

        ThreadContext.push("stateChanged");

        try {

            logger.debug("{}: {}", dc, value);

            ((AbstractSensorContainer) dc).stateChanged(value, null);

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Broadcast the exception notification.
     *
     * @param dc Device container for the sensor whose state has changed.
     * @param t Exception.
     */
    private void stateChanged(final DeviceContainer dc, final Throwable t) {

        ThreadContext.push("stateChanged");

        try {

            logger.debug(dc, t);

            try {

                ((AbstractSensorContainer) dc).stateChanged(null, t);

            } catch (ClassCastException ex) {

                logger.error("Oops... not a sensor", t);
            }

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Get a path object for a given address.
     *
     * @param address Address to get the path for.
     * @return A path object for a given address.
     * @exception NoSuchElementException if there's no path for given address.
     */
    public final OWPath getDevicePath(final String address) {

        for (var pd : path2device.entrySet()) {

            var path = pd.getKey();
            var address2dcForPath = pd.getValue();

            if (address2dcForPath.containsKey(address)) {
                return path;
            }
        }

        throw new NoSuchElementException("No path found for '" + address + "'");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JmxDescriptor getJmxDescriptor() {

        return new JmxDescriptor(
                "dz",
                getClass().getSimpleName(),
                adapterPort,
                "1-Wire Device Factory at " + speed2name.get(adapterSpeed) + " speed on " + adapterPort);
    }

    /**
     * Volatile switch state representation.
     */
    protected static class SwitchState {

        /**
         * True if the switch supports smart operation.
         */
        boolean smart = false;

        /**
         * Switch state. VT: NOTE: Extend this for cases like DS2408 (where the number of channels is not 2).
         */
        boolean[] state = { false, false };

        /**
         * @return String representation of the switch state.
         */
        @Override
        public final String toString() {

            var sb = new StringBuilder("[");
            sb.append((smart ? "smart" : "dumb"));

            sb.append("][");

            for (var idx = 0; idx < state.length; idx++) {

                if (idx != 0) {

                    sb.append(",");
                }

                sb.append(state[idx]);
            }

            sb.append("]");

            return sb.toString();
        }
    }

    @SuppressWarnings("squid:S110")
    private class OwapiSensorProxy extends SensorProxy implements DataSink<Double>, JmxAware, OneWireNetworkEventListener {

        public OwapiSensorProxy(String address, int pollIntervalMillis, SensorType type, OneWireNetworkMonitor monitor) {

            super(address, pollIntervalMillis, type);

            monitor.addListener(this);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public JmxDescriptor getJmxDescriptor() {

            return new JmxDescriptor(
                    "dz",
                    getClass().getSimpleName(),
                    type.type + getAddress(),
                    "1-Wire " + type.description + " sensor , adress " + getAddress());
        }

        @Override
        public void networkArrival(OneWireNetworkEvent e) {
            // Nothing to do here, the arrival will be handled asynchronously by getSensorSignal() shortly
        }

        @Override
        public void networkDeparture(OneWireNetworkEvent e) {

            ThreadContext.push("networkDeparture");

            try {

                if (container == null) {

                    // We've already been eliminated in this round, nothing to do
                    return;
                }

                if (e.address.equals(container.getAddress())) {

                    logger.debug("Signal: {}", e);

                    consume(new DataSample<>(
                            System.currentTimeMillis(), type + getAddress(), type + getAddress(),
                            null, new OneWireIOException("Departed")));

                    logger.debug("Discarded container: #{}", () -> Integer.toHexString(container.hashCode()));
                    container = null;
                }

            } finally {
                ThreadContext.pop();
            }
        }

        @Override
        public void networkFault(OneWireNetworkEvent e, String message) {

            ThreadContext.push("networkFault");

            try {

                // This is an event pertinent to everyone

                consume(new DataSample<>(
                        System.currentTimeMillis(), type + getAddress(), type + getAddress(),
                        null, new OneWireIOException(message)));

            } finally {
                ThreadContext.pop();
            }
        }
    }

    private static class OwapiSingleSwitchProxy extends SingleSwitchProxy<OneWireSwitchContainer> {

        public OwapiSingleSwitchProxy(ContainerMap address2dcGlobal, StringChannelAddress address) {
            super(address2dcGlobal, address);
        }

        @Override
        public synchronized boolean getState() throws IOException {

            ThreadContext.push("getState");

            try {

                OneWireSwitchContainer sc = getContainer(address.hardwareAddress);

                if (sc == null) {
                    throw new IOException("No container found for " + address + ", assuming not present");
                }

                return sc.read(Integer.parseInt(address.channel));

            } finally {
                ThreadContext.pop();
            }
        }

        @Override
        public synchronized void setState(boolean state) throws IOException {

            ThreadContext.push("setState");

            try {

                OneWireSwitchContainer sc = getContainer(address.hardwareAddress);

                if (sc == null) {
                    throw new IOException("No container found for " + address + ", assuming not present");
                }

                sc.write(Integer.parseInt(address.channel), state);

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
                    "1-Wire single switch proxy");
        }

        @Override
        public int compareTo(Addressable o) {
            // Can't afford to collide with the wrapper
            return (getClass().getName() + getAddress()).compareTo((o.getClass().getName() + o.getAddress()));
        }
    }

    @Override
    protected Switch createSingleSwitchProxy(ContainerMap address2dcGlobal, StringChannelAddress switchAddress) {
      return new OwapiSingleSwitchProxy(address2dcGlobal, switchAddress);
    }
}
