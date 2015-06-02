package net.sf.dz3.device.factory;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.sf.dz3.device.sensor.AnalogSensor;
import net.sf.dz3.device.sensor.DeviceContainer;
import net.sf.dz3.device.sensor.DeviceFactory;
import net.sf.dz3.device.sensor.PrototypeContainer;
import net.sf.dz3.device.sensor.SensorType;
import net.sf.dz3.device.sensor.Switch;
import net.sf.dz3.device.sensor.impl.AbstractAnalogSensor;
import net.sf.dz3.device.sensor.impl.AbstractDeviceContainer;
import net.sf.dz3.device.sensor.impl.ContainerMap;
import net.sf.dz3.device.sensor.impl.StringChannelAddress;
import net.sf.jukebox.datastream.logger.impl.DataBroadcaster;
import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.datastream.signal.model.DataSink;
import net.sf.jukebox.datastream.signal.model.DataSource;
import net.sf.jukebox.jmx.JmxAttribute;
import net.sf.jukebox.jmx.JmxAware;
import net.sf.jukebox.service.ActiveService;

import org.apache.log4j.NDC;

/**
 * An entity capable of resolving a device by address.
 * 
 * Loosely based on {@code net.sf.dz.daemon.onewire.owapi.OneWireServer}.
 * 
 * This class behaves like a singleton, but is not built like one - the intent is to instantiate
 * it with Spring Framework, which will take care of creating as few instances as needed.
 * 
 * @param <SwitchContainer> Implementation class of the hardware dependent switch container.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko 2001-2010
 */
public abstract class AbstractDeviceFactory<SwitchContainer> extends ActiveService implements DeviceFactory {

    protected final DataBroadcaster<Double> dataBroadcaster = new DataBroadcaster<Double>();
    
    /**
     * Constant to use as a key for humidity data.
     */
    public static final String DATA_HUM = "humidity";
    
    /**
     * Constant to use as a key for temperature data.
     */
    public static final String DATA_TEMP = "temperature";
    
    /**
     * Constant to use as a key for switch data.
     */
    public static final String DATA_SWITCH = "switch";
    
    /**
     * Read/write lock controlling the exclusive access to hardware devices.
     * 
     * Note that the lock is constructed with an argument, otherwise fairness is not supported.
     */
    protected ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    
    /**
     * Device map. The key is the address, the value is the device container.
     */
    protected final ContainerMap address2dcGlobal = new ContainerMap();
    
    protected Map<String, SwitchChannelSplitter> address2proxy = new TreeMap<String, SwitchChannelSplitter>();
    
    /**
     * Data map.
     */
    protected DataMap dataMap = new DataMap();
    
    public AbstractDeviceFactory() {
        super();
    }

    public AbstractDeviceFactory(ThreadGroup tg, ThreadFactory tf) {
        super(tg, tf);
    }

    public AbstractDeviceFactory(ThreadFactory tf) {
        super(tf);
    }

    /**
     * Get a server lock.
     *
     * @return The server lock.
     */
    public final ReentrantReadWriteLock getLock() {
        return lock;
    }

    /**
     * Get an instance of a temperature sensor.
     * 
     * @param address Hardware address.
     * 
     * @return An instance of a temperature sensor, unconditionally. In case when
     * the device with a given address is not present on a bus, the instance returned will keep
     * producing {@link DataSample error samples} over and over, with "Not Present" being the error. 
     */
    @Override
    public final synchronized AnalogSensor getTemperatureSensor(String address) {
        
        NDC.push("getTemperatureSensor");
        
        try {
            
            return getSensor(address, SensorType.TEMPERATURE);
            
        } finally {
            NDC.pop();
        }
    }

    /**
     * Get an instance of a humidity sensor.
     * 
     * @param address Hardware address.
     * 
     * @return An instance of a humidity sensor, unconditionally. In case when
     * the device with a given address is not present on a bus, the instance returned will keep
     * producing {@link DataSample error samples} over and over, with "Not Present" being the error. 
     */
    @Override
    public final synchronized AnalogSensor getHumiditySensor(String address) {
        
        NDC.push("getHumiditySensor");
        
        try {
            
            return getSensor(address, SensorType.HUMIDITY);
            
        } finally {
            NDC.pop();
        }
    }

    protected abstract AnalogSensor createSensorProxy(String address, SensorType type);
    
    protected AnalogSensor getSensor(String address, SensorType type) {
        
        NDC.push("getSensor");
        
        try {
            
            logger.debug("Looking for " + address + " (type " + type + ")");
            
            Set<DeviceContainer> devices = address2dcGlobal.get(address);
            
            if (devices == null) {
                
                // Tough luck. The sensor hasn't been discovered yet - this is a normal situation at startup.
                return createSensorProxy(address, type);
            }
            
            for (Iterator<DeviceContainer> i = devices.iterator(); i.hasNext(); ) {
                
                DeviceContainer dc = i.next();
                logger.info("Found: " + dc + ", " + dc.getType());
                
                if (type.equals(dc.getType())) {

                    // Voila, we already have it
                    return (AnalogSensor) dc;
                }

                if (dc.getType().equals(SensorType.PROTOTYPE)) {

                    return ((PrototypeContainer) dc).getSensor(address, type);
                }
            }
            
            logger.warn("Address " + address + " present, but no " + type.description + " sensors were found at this address, likely configuration error. Creating proxy container anyway");
            return createSensorProxy(address, type);
            
        } finally {
            NDC.pop();
        }
    }

    @Override
    public void addConsumer(DataSink<Double> consumer) {
    
        dataBroadcaster.addConsumer(consumer);
    }

    @Override
    public void removeConsumer(DataSink<Double> consumer) {
    
        dataBroadcaster.removeConsumer(consumer);
    }

    /**
     * @return Size of {@link #address2path} map.
     * @deprecated This method is intended to help finding a memory leak and has no other reason to exist.
     */
    @JmxAttribute(description = "address2dcGlobal size")
    public synchronized int getAddress2dcGlobalSize() {
        return address2dcGlobal.size();
    }

    /**
     * Instrumentation method for obtaining all device addresses that are currently present
     * on the bus.
     * 
     * @return List of device addresses as string.
     */
    @JmxAttribute(description = "All hardware addresses on the bus")
    public synchronized String[] getAddresses() {
    
        String[] result = new String[address2dcGlobal.size()];
        
        int offset = 0;
        for (Iterator<String> i = address2dcGlobal.iterator(); i.hasNext(); ) {
            
            result[offset++] = i.next();
        }
        
        return result;
    }

    public final class SwitchChannelSplitter {

        /**
         * Single channel switch map.
         */
        private Map<StringChannelAddress, Switch> address2switch = new TreeMap<StringChannelAddress, Switch>();
        
        public synchronized Switch getSwitch(StringChannelAddress switchAddress) {

            Switch singleChannelSwitch = address2switch.get(switchAddress);
            
            if (singleChannelSwitch == null) {

                singleChannelSwitch = createSingleSwitchProxy(address2dcGlobal, switchAddress);
                address2switch.put(switchAddress, singleChannelSwitch);
            }
            
            return singleChannelSwitch;
        }
    }

    protected abstract Switch createSingleSwitchProxy(ContainerMap address2dcGlobal, StringChannelAddress switchAddress);
    
    protected abstract class SensorProxy extends AbstractAnalogSensor implements DataSink<Double> {

        /**
         * Sensor type.
         */
        protected final SensorType type;
        
        /**
         * Container to proxy when it becomes available.
         * 
         * VT: FIXME: May need to change visibility back to private
         * and add a method to check and reset the state from subclasses.
         */
        protected AbstractDeviceContainer container = null;

        public SensorProxy(String address, int pollIntervalMillis, SensorType type) {

            super(address, pollIntervalMillis);
            
            if (!SensorType.TEMPERATURE.equals(type) && !SensorType.HUMIDITY.equals(type)) {
                
                throw new IllegalArgumentException("Don't know how to handle type '" + type + "'");
            }
            
            this.type = type;
        }

        @SuppressWarnings("unchecked")
        @Override
        public final synchronized DataSample<Double> getSensorSignal() throws IOException {
            
            NDC.push("getSensorSignal@" + Integer.toHexString(hashCode()));
            
            try {
                
                if (container != null) {
                    
                    throw new IllegalStateException("This shouldn't have happened");
                }

                Set<DeviceContainer> devices = address2dcGlobal.get(getAddress());
                
                if (devices == null) {
                    
                    // Tough luck. The sensor hasn't been discovered yet - this is a normal situation at startup.
                    return new DataSample<Double>(
                            System.currentTimeMillis(), type + getAddress(), type + getAddress(),
                            null, new IllegalStateException("Not Present"));
                }
                
                for (Iterator<DeviceContainer> i = devices.iterator(); i.hasNext(); ) {
                    
                    DeviceContainer dc = i.next();
                    logger.info("Found: " + dc + ", " + dc.getType() + ", #" + Integer.toHexString(dc.hashCode()));
                    
                    if (type.equals(dc.getType())) {

                        // Yes!!!
                        logger.info(type + getAddress() + " arrived, starting proxying");
                        this.container = (AbstractDeviceContainer) dc;
                        
                        ((DataSource<Double>)dc).addConsumer(this);

                        return new DataSample<Double>(
                                System.currentTimeMillis(), type + getAddress(), type + getAddress(),
                                null, new IllegalStateException("Found, next reading will be good"));
                    }
                    
                    if (dc.getType().equals(SensorType.PROTOTYPE)) {
                        
                        logger.info(type + getAddress() + ": found prototype");
                        this.container = (AbstractDeviceContainer) ((PrototypeContainer) dc).getSensor(getAddress(), type);

                        ((DataSource<Double>) this.container).addConsumer(this);

                        return new DataSample<Double>(
                                System.currentTimeMillis(), type + getAddress(), type + getAddress(),
                                null, new IllegalStateException("Found, next reading will be good"));
                    }
                }
                
                return new DataSample<Double>(
                        System.currentTimeMillis(), type + getAddress(), type + getAddress(),
                        null, new IllegalStateException("Address is present, but no " + type.description + " sensors found - check configuration"));
                
            } finally {
                NDC.pop();
            }
            
        }

        @Override
        protected void shutdown() throws Throwable {
            // TODO Auto-generated method stub
            
        }

        @Override
        protected void startup() throws Throwable {
        }

        @Override
        protected final void execute() {

            if (getPollInterval() < 0) {

                throw new IllegalStateException("Negative poll interval (" + getPollInterval() + ")???");
            }
            
            NDC.push("execute@" + Integer.toHexString(hashCode()) + "@" + getAddress());

            try {

                while (isEnabled()) {
                    
                    if (container != null) {
                        
                        // No need to do anything, data will be automatically rebroadcast
                    } else {
                        
                        // Kick the logic to check if the actual device is already available
                        getSensorSignal();
                    }

                    //logger.debug("Current signal: " + currentSignal);

                    Thread.sleep(getPollInterval());
                }

            } catch (Throwable t) {
                logger.fatal("Unexpected problem, shutting down:", t);
            }
        }

        @Override
        public final synchronized void consume(DataSample<Double> signal) {
            
            NDC.push("consume@" + Integer.toHexString(hashCode()));
            
            try {
            
                if (container == null) {
                    throw new IllegalStateException("How did we end up here?");
                }

                logger.debug("Rebroadcast: " + signal);
                logger.debug("Container: #" + Integer.toHexString(container.hashCode()));

                currentSignal = signal;
                broadcast(signal);
                
            } finally {
                NDC.pop();
            }
        }
    }
}
