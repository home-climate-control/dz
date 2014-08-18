package net.sf.dz3.device.sensor.impl;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import net.sf.dz3.device.sensor.DeviceContainer;

/**
 * Container address2container.
 * <p>
 * The key is the device address, the value is a set of {@link DeviceContainer
 * device containers}.
 * <p>
 * This class is introduced to improve reliability of transition between the
 * version of code where one physical device corresponds to one logical device,
 * and the version of code where one physical device may correspond to several
 * logical devices of different types (such as DS2438 posing as both temperature
 * and humidity container).
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2010
 */
public class ContainerMap {

    private final Logger logger = Logger.getLogger(getClass());
    
    /**
     * Device address to device container instance map.
     */
    private Map<String, Set<DeviceContainer>> address2container = new TreeMap<String, Set<DeviceContainer>>();
    
    /**
     * Translate logical address into hardware address.
     * 
     * @param address Logical address (may include colon and channel address
     */
    public String getHardwareAddress(String address) {
        
        NDC.push("getHardwareAddress");
        
        try {
            
            if (address == null) {
                throw new IllegalArgumentException("address can't be null");
            }

            if (address.indexOf(':') == -1) {

                return address;

            }

            logger.debug("extracting hardware address from channel address");

            // String channel will do because it can handle integer channel
            // (but not the other way around)
            StringChannelAddress channelAddress = new StringChannelAddress(address);

            logger.debug(address + " => " + channelAddress.hardwareAddress);

            return channelAddress.hardwareAddress;

        } finally {
            NDC.pop();
        }
    }

    /**
     * Map the device container to the address. Existing mapping, if any, is not
     * replaced, but augmented.
     * 
     * @param dc Device container to address2container.
     */
    public void add(DeviceContainer dc) {
        
        NDC.push("add");
        
        try {

            logger.info("device: " + dc.getAddress());
            
            String address = getHardwareAddress(dc.getAddress());
            Set<DeviceContainer> s = get(address);

            if (s == null) {

                s = new TreeSet<DeviceContainer>();

                address2container.put(address, s);
            }

            s.add(dc);
            
            //dump();
            
        } finally {
            NDC.pop();
        }
    }

    /**
     * Get the mapping.
     * 
     * @param address Address to get the container for.
     * @return Set of device containers associated with the given address.
     */
    public Set<DeviceContainer> get(String address) {
        
        NDC.push("get(" + address + ")");
        
        try {

            address = getHardwareAddress(address);

            Set<DeviceContainer> found = address2container.get(address);
            
            if (found == null) {
                
                logger.debug("Found no devices for hardware address " + address);
                
            } else {
            
                logger.debug("Found " + found.size() + " devices for hardware address " + address);
            }
            
            //dump();
            
            return found;

        } finally {
            NDC.pop();
        }
    }

    /**
     * Remove the mapping.
     * 
     * @param address Address to remove the mapping for.
     * @return Old value associated with the address.
     */
    public Set<DeviceContainer> remove(String address) {

        return address2container.remove(address);
    }

    /**
     * @return {@code true} if there are no mappings in this container.
     */
    public boolean isEmpty() {

        return address2container.isEmpty();
    }

    /**
     * @return Iterator on the addresses of devices in this container.
     */
    public Iterator<String> iterator() {

        return address2container.keySet().iterator();
    }

    /**
     * Check if we have anything for a given address.
     * 
     * @param address Address to check.
     * @return {@code true} if we already know something about this address.
     */
    public boolean containsKey(String address) {

        return address2container.containsKey(address);
    }
    
    /**
     * Get the size of the container map.
     * 
     * @return Container size.
     */
    public synchronized int size() {
        
        return address2container.size();
    }
    
    /**
     * Dump the contents into the log at debug level.
     */
    public void dump() {
        
        NDC.push("dump#" + Integer.toHexString(hashCode()));
        
        try {
            
            logger.debug(address2container.size() + " addresses");
        
            for (Iterator<String> i = address2container.keySet().iterator(); i.hasNext(); ) {

                String address = i.next();
                Set<DeviceContainer> devices = address2container.get(address);

                StringBuilder sb = new StringBuilder();

                sb.append(address).append(" ");

                if (devices == null) {

                    sb.append("NULL???");

                } else {

                    sb.append(" (" + devices.size() + " devices) ");

                    for (Iterator<DeviceContainer> i2 = devices.iterator(); i2.hasNext(); ) {

                        sb.append(i2.next().getAddress());

                        if (i2.hasNext()) {
                            sb.append(", ");
                        }
                    }
                }

                logger.debug(sb.toString());
            }
            
        } finally {
            NDC.pop();
        }
    }
}
