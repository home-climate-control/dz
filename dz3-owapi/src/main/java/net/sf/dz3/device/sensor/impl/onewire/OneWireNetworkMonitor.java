package net.sf.dz3.device.sensor.impl.onewire;

import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.container.OneWireContainer;
import com.dalsemi.onewire.container.OneWireContainer1F;
import com.dalsemi.onewire.utils.OWPath;

import net.sf.dz3.instrumentation.Marker;
import net.sf.jukebox.jmx.JmxAttribute;
import net.sf.jukebox.jmx.JmxDescriptor;
import net.sf.jukebox.sem.EventSemaphore;
import net.sf.jukebox.sem.SemaphoreTimeoutException;
import net.sf.jukebox.service.ActiveService;
import net.sf.jukebox.util.CollectionSynchronizer;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.logging.log4j.ThreadContext;

/**
 * 1-Wire&reg; network monitor.
 * <p>
 * The difference between this class and DalSemi's {@code
 * com.dalsemi.onewire.utils.NetworkMonitor} is that this class not only
 * searches the currently open branches, but manipulates the MicroLAN couplers
 * (DS2409) it finds in order to get a complete network map. It may not be
 * desirable for a generic network browser, but it is perfectly OK in the
 * context of this project.
 * <p>
 * Note that DS2409 devices cannot be used as payload switches with this code,
 * only as branch couplers.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2018
 */
public class OneWireNetworkMonitor extends ActiveService {

    /**
     * Adapter to monitor.
     */
    private DSPortAdapter adapter;

    /**
     * Lock to use for mutual exclusive access to the adapter. I hope there'll
     * be a day when the adapter itself will provide this lock...
     */
    private ReentrantReadWriteLock lock;

    /**
     * Device map. The key is the address as string, the value is the device
     * container for that address.
     */
    private Map<String, OneWireContainer> address2device = new TreeMap<String, OneWireContainer>();

    /**
     * Path map. The key is the address as string, the value is the path to the
     * device with that address.
     */
    private Map<String, OWPath> address2path = new TreeMap<String, OWPath>();

    /**
     * Listener set.
     */
    private Set<OneWireNetworkEventListener> listenerSet = new HashSet<OneWireNetworkEventListener>();

    /**
     * "Force rescan" semaphore.
     * <p>
     * When this semaphore is posted, {@link #browse() browse()} is called
     * immediately, as opposed to once in a timeout.
     * <p>
     * This semaphore is triggered by calling {@link #rescan rescan()}.
     *
     * @see #rescanComplete
     */
    private EventSemaphore rescanNow = new EventSemaphore();

    /**
     * "Rescan complete" semaphore.
     * <p>
     * This semaphore is triggered whenever {@link #browse() browse()} finishes.
     * {@link #rescan rescan()} returns this semaphore, for convenience of the
     * callers.
     */
    private EventSemaphore rescanComplete = new EventSemaphore();

    /**
     * Rescan timeout. Default is 1 minute.
     */
    private long timeout = 60000;

    /**
     * A debug variable - set to true whenever a rescan is forced (as opposed to
     * the rescan semaphore timing out).
     */
    private boolean forcedRescan = false;

    /**
     * Create an instance.
     *
     * @param adapter Adapter to work with. Can't be {@code null}.
     * @param lock Lock to use. Can't be {@code null}.
     */
    public OneWireNetworkMonitor(DSPortAdapter adapter, ReentrantReadWriteLock lock) {

        if (adapter == null || lock == null) {

            throw new IllegalArgumentException("One of parameters is null: adapter: " + adapter + ", lock: " + lock);
        }

        this.adapter = adapter;
        this.lock = lock;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void startup() throws Throwable {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void execute() throws Throwable {
        
        ThreadContext.push("execute");
        
        try {

            while (isEnabled()) {

                try {

                    browse();

                    // Currently, browse() takes 10+ seconds. Let's give them a
                    // break

                    try {

                        rescanNow.waitFor(timeout);
                        logger.debug("Rescan requested");

                        forcedRescan = true;

                    } catch (SemaphoreTimeoutException ignored) {

                        // Don't do anything, this is a normal outcome when the
                        // semaphore times out, which happens every time the
                        // rescan() wasn't called.

                        forcedRescan = false;
                    }

                } catch (Throwable t) {

                    String message = t.getMessage();

                    if (message != null && message.startsWith("Error short")) {

                        logger.fatal("1-Wire Network shorted out!!!", t);

                        // VT: FIXME: This is a critical failure. All the
                        // listeners must be notified immediately - this most
                        // probably means loss of all sensors, and what's worse,
                        // this means loss of control over HVAC actuator
                        // devices, though the devices themselves have to be
                        // powered and therefore keep their state - in other
                        // words, if the HVAC is on, it will stay on. Very Bad
                        // Thing (TM).

                        // Well, and of course, it *has* to be this inconvenient
                        // - the only place where a network short can be
                        // detected is where you actually *write* something to
                        // it. Both places are outside of normal control flow,
                        // and they're asynchronous.

                        handleShortCircuit();

                        // Now, the short circuit is a condition that is
                        // detected at the very beginning of browse(), very
                        // fast. Let to its own devices, this will cycle
                        // indefinitely, so it would be a good idea to restrict
                        // the cycle rate.

                        Thread.sleep(1000);

                    } else {

                        logger.warn("Browse failure:", t);
                    }
                }
            }
        
        } finally {
            ThreadContext.pop();
            ThreadContext.clearStack();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void shutdown() throws Throwable {
        
        logger.info("stopped");
    }

    /**
     * Find all devices on 1-Wire&reg; network.
     * <p>
     * This is an intrusive search that is going to shuffle all the MicroLAN
     * couplers on the network.
     *
     * @throws Throwable if anything goes wrong.
     */
    private void browse() throws Throwable {
        
        ThreadContext.push("browse");
        Marker m = new Marker("browse");
        
        lock.writeLock().lock();
        m.checkpoint("got lock");
        
        try {

            if (forcedRescan) {

                logger.info("Forced rescan");
            }


            Map<String, OneWireContainer> address2deviceLocal = new TreeMap<String, OneWireContainer>();
            Map<String, OWPath> address2pathLocal = new TreeMap<String, OWPath>();

            ThreadContext.push("browseProper");
            Marker m2 = new Marker("browseProper");

            try {


                adapter.setSearchAllDevices();
                adapter.targetAllFamilies();

                OWPath rootPath = new OWPath(adapter);

                browse(rootPath, address2deviceLocal, address2pathLocal);

            } finally {

                m2.close();
                ThreadContext.pop();
            }
            
            ThreadContext.push("handleChanges");
            Marker m3 = new Marker("handleChanges");
            
            try {

                handleDepartures(address2deviceLocal, address2pathLocal);
                handleArrivals(address2deviceLocal, address2pathLocal);

                address2device = address2deviceLocal;
                address2path = address2pathLocal;

            } finally {
                
                m3.close();
                ThreadContext.pop();
            }

            logger.debug("Rescan complete");
            rescanComplete.post();

            if (address2deviceLocal.isEmpty()) {
                logger.warn("No devices found, waiting 10 seconds to avoid runaway looping");
                Thread.sleep(10000);
            }

        } finally {

            forcedRescan = false;
            lock.writeLock().unlock();

            m.close();
            ThreadContext.pop();
            ThreadContext.clearStack();
        }
    }

    /**
     * Browse the path.
     *
     * @param path Path to start browsing at.
     * @param address2deviceLocal Input/output parameter to fill out. The key is
     * the address as string, the value is the device container.
     * @param address2pathLocal Input/output parameter to fill out. The key is
     * the address as string, the value is the path to that address.
     * @throws Throwable if anything goes wrong.
     */
    private void browse(OWPath path, Map<String, OneWireContainer> address2deviceLocal,
            Map<String, OWPath> address2pathLocal) throws Throwable {

        closeAllPaths();

        path.open();

        List<OWPath> switchList = new LinkedList<OWPath>();

        for (Enumeration<OneWireContainer> e = adapter.getAllDeviceContainers(); e.hasMoreElements();) {

            OneWireContainer owc = e.nextElement();
            String address = owc.getAddressAsString();

            // This device might have been discovered already

            if (address2deviceLocal.get(address) != null) {

                continue;
            }

            logger.debug("Found: " + owc.getName() + " " + address + " at " + path);

            address2deviceLocal.put(address, owc);
            address2pathLocal.put(address, path);

            if (owc instanceof OneWireContainer1F) {

                OWPath channel1 = new OWPath(adapter, path);

                channel1.add(owc, 0);

                switchList.add(channel1);

                OWPath channel2 = new OWPath(adapter, path);

                channel2.add(owc, 1);

                switchList.add(channel2);
            }
        }

        for (Iterator<OWPath> i = switchList.iterator(); i.hasNext();) {

            OWPath branchPath = i.next();

            browse(branchPath, address2deviceLocal, address2pathLocal);
        }
    }

    /**
     * Rescan the network now.
     * <p>
     * If a synchronous invocation is desired, this method may be invoked as
     * follows:
     *
     * <pre>
     * monitor.rescan().waitFor();
     * </pre>
     *
     * <strong>WARNING:</strong> Do not attempt to wait on the semaphore unless
     * you initiated the rescan - you may end up waiting for entire timeout
     * interval.
     *
     * @return {@link #rescanComplete A semaphore} that is triggered when
     * {@link #browse() browse()} is complete.
     */
    public EventSemaphore rescan() {

        rescanNow.post();

        return rescanComplete;
    }

    /**
     * Handle the new device arrival. Compare new maps with existing maps, and
     * issue notifications about devices that have just arrived, if any.
     *
     * @param newDeviceMap Full device address to device container map, possibly
     * containing devices that have just arrived.
     * @param newPathMap Full device address to device path map, possibly
     * containing devices that have just arrived.
     */
    private void handleArrivals(final Map<String, OneWireContainer> newDeviceMap, final Map<String, OWPath> newPathMap) {

        for (Iterator<String> i = newDeviceMap.keySet().iterator(); i.hasNext();) {

            String address = i.next();

            if (!address2device.containsKey(address)) {

                logger.info("Arrived: " + newDeviceMap.get(address).getName() + " " + address + " on "
                        + newPathMap.get(address));

                OneWireNetworkEvent e = new OwapiNetworkEvent(this, adapter, address, newPathMap.get(address));

                for (Iterator<OneWireNetworkEventListener> li = (new CollectionSynchronizer<OneWireNetworkEventListener>())
                        .copy(listenerSet).iterator(); li.hasNext();) {

                    try {
                        li.next().networkArrival(e);
                    } catch (Throwable t) {
                        logger.error("Unexpected exception", t);
                    }
                }
            }
        }
    }

    /**
     * Handle device departure. Compare new maps with existing maps, and
     * issue notifications about devices that have just departed, if any.
     *
     * @param newDeviceMap Full device address to device container map, possibly
     * containing devices that have just departed.
     * @param newPathMap Full device address to device path map, possibly
     * containing devices that have just departed.
     */
    private void handleDepartures(final Map<String, OneWireContainer> newDeviceMap, final Map<String, OWPath> newPathMap) {

        for (Iterator<String> i = address2device.keySet().iterator(); i.hasNext();) {

            String address = i.next();

            OWPath oldPath = address2path.get(address);
            OWPath newPath = newPathMap.get(address);

            if (oldPath == null) {

                // This means that we're actually seeing the arrival, not
                // departure

                continue;
            }

            if (newPath == null) {

                // The device has departed

                logger.warn("Departed: " + address + " from " + oldPath);

                OneWireNetworkEvent e = new OwapiNetworkEvent(this, adapter, address, oldPath);

                for (Iterator<OneWireNetworkEventListener> li = (new CollectionSynchronizer<OneWireNetworkEventListener>())
                        .copy(listenerSet).iterator(); li.hasNext();) {

                    try {
                        li.next().networkDeparture(e);
                    } catch (Throwable t) {
                        logger.error("Unexpected exception", t);
                    }
                }

                continue;
            }

            // At this point, oldPath can't be null

            if (!oldPath.equals(newPath)) {

                // The path has changed

                logger.info("Moved: " + address + " from " + oldPath + " to " + newPath);

                OneWireNetworkEvent e = new OwapiNetworkEvent(this, adapter, address, oldPath);

                for (Iterator<OneWireNetworkEventListener> li = (new CollectionSynchronizer<OneWireNetworkEventListener>())
                        .copy(listenerSet).iterator(); li.hasNext();) {

                    try {
                        li.next().networkDeparture(e);
                    } catch (Throwable t) {
                        logger.error("Unexpected exception", t);
                    }
                }

                e = new OwapiNetworkEvent(this, adapter, address, newPath);

                for (Iterator<OneWireNetworkEventListener> li = (new CollectionSynchronizer<OneWireNetworkEventListener>())
                        .copy(listenerSet).iterator(); li.hasNext();) {

                    try {
                        li.next().networkArrival(e);
                    } catch (Throwable t) {
                        logger.error("Unexpected exception", t);
                    }
                }
            }
        }
    }

    /**
     * Handle a short circuit.
     */
    private void handleShortCircuit() {

        for (Iterator<String> i = address2device.keySet().iterator(); i.hasNext();) {

            String address = i.next();
            OneWireNetworkEvent e = new OwapiNetworkEvent(this, adapter, address, null);

            for (Iterator<OneWireNetworkEventListener> li = (new CollectionSynchronizer<OneWireNetworkEventListener>())
                    .copy(listenerSet).iterator(); li.hasNext();) {

                try {
                    li.next().networkFault(e, "1-Wire network short circuit");
                } catch (Throwable t) {
                    logger.error("Unexpected exception", t);
                }
            }
        }
    }

    /**
     * Add a listener.
     *
     * @param listener Listener to add.
     */
    public void addListener(OneWireNetworkEventListener listener) {

        synchronized (listenerSet) {

            listenerSet.add(listener);
        }
    }

    /**
     * Remove a listener.
     *
     * @param listener Listener to remove.
     */
    public void removeListener(OneWireNetworkEventListener listener) {

        synchronized (listenerSet) {

            listenerSet.remove(listener);
        }
    }

    /**
     * Close all open device paths. VT: FIXME: This is a shortcut using DS2409
     * specific hardware commands to close all open paths. Must be modified if a
     * different adapter is ever used.
     *
     * @throws Throwable if anything goes wrong.
     */
    private void closeAllPaths() throws Throwable {

        // DS2409 specific - skip, all lines off

        adapter.reset();
        adapter.putByte(0x00CC);
        adapter.putByte(0x0066);
        adapter.getByte();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JmxDescriptor getJmxDescriptor() {

        return new JmxDescriptor(
                "dz",
                getClass().getSimpleName(),
                Integer.toHexString(hashCode()),
                "1-Wire Network Monitor");
    }
    
    /**
     * @deprecated This method is intended to help finding a memory leak and has no other reason to exist.
     */
    @JmxAttribute(description = "listenerSet size")
    public int getListenerSetSize() {
        return listenerSet.size();
    }
    /**
     * Instrumentation method to track down a memory leak.
     * 
     * @return Size of {@link #address2device} map.
     * @deprecated This method is intended to help finding a memory leak and has no other reason to exist.
     */
    @JmxAttribute(description = "address2device size")
    public synchronized int getAddress2deviceSize() {
        return address2device.size();
    }

    /**
     * Instrumentation method to track down a memory leak.
     * 
     * @return Size of {@link #address2path} map.
     * @deprecated This method is intended to help finding a memory leak and has no other reason to exist.
     */
    @JmxAttribute(description = "address2path size")
    public synchronized int getAddress2pathSize() {
        return address2path.size();
    }
}
