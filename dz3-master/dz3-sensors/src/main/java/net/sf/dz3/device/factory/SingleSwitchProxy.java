package net.sf.dz3.device.factory;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import net.sf.dz3.device.sensor.DeviceContainer;
import net.sf.dz3.device.sensor.PrototypeContainer;
import net.sf.dz3.device.sensor.SensorType;
import net.sf.dz3.device.sensor.Switch;
import net.sf.dz3.device.sensor.impl.ContainerMap;
import net.sf.dz3.device.sensor.impl.StringChannelAddress;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

/**
 * A proxy for a single channel switch device.
 *  
 * @param <SwitchContainerType> Implementation class of the hardware dependent switch container.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko 2001-2010
 */
public abstract class SingleSwitchProxy<SwitchContainerType> implements Switch {

    protected final Logger logger = Logger.getLogger(getClass());
    
    protected final ContainerMap address2dcGlobal;
    protected final StringChannelAddress address;
    
    public SingleSwitchProxy(ContainerMap address2dcGlobal, StringChannelAddress address) {
        
        this.address2dcGlobal = address2dcGlobal;
        this.address = address;
    }

    @Override
    public abstract boolean getState() throws IOException;

    @Override
    public abstract void setState(boolean state) throws IOException;
    
    /**
     * Get the device container.
     * 
     * @param address Hardware address.
     * 
     * @return Device container.
     * 
     * @exception IOException if the device container is not present.
     */
    @SuppressWarnings("unchecked")
    protected final SwitchContainerType getContainer(String address) throws IOException {
        
        NDC.push("getContainer");
        
        try {
            
            Set<DeviceContainer> devices = address2dcGlobal.get(address);
            
            if (devices == null) {
                
                throw new IOException("No container found for " + address + ", assuming not present");
            }
            
            for (Iterator<DeviceContainer> i = devices.iterator(); i.hasNext(); ) {
                
                DeviceContainer dc = i.next();
                logger.debug("Found: " + dc + ", " + dc.getType());
                
                if (SensorType.SWITCH.equals(dc.getType())) {

                    // Voila, we already have it
                    return (SwitchContainerType) dc;
                }
                                
                if (dc.getType().equals(SensorType.PROTOTYPE)) {

                    return (SwitchContainerType) ((PrototypeContainer) dc).getSwitch(address);
                }
            }
            
            throw new IOException("Device present at " + address + ", not a switch nor a prototype");
            
        } finally {
            NDC.pop();
        }
    }

    @Override
    public final String getAddress() {
        
        return address.toString();
    }
    
}
