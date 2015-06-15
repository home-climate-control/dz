package net.sf.dz3.device.sensor.impl.xbee;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import net.sf.dz3.device.sensor.AnalogSensor;
import net.sf.dz3.device.sensor.PrototypeContainer;
import net.sf.dz3.device.sensor.SensorType;
import net.sf.dz3.device.sensor.Switch;
import net.sf.dz3.device.sensor.impl.AbstractDeviceContainer;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import com.rapplogic.xbee.api.RemoteAtRequest;
import com.rapplogic.xbee.api.XBeeAddress64;
import com.rapplogic.xbee.api.XBeeException;
import com.rapplogic.xbee.api.XBeeResponse;

/**
 * XBee device container.
 *   
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko 2001-2010
 */
public final class XBeeDeviceContainer extends AbstractDeviceContainer implements PrototypeContainer {
    
    private final Logger logger = Logger.getLogger(getClass()); 

    private final XBeeDeviceFactory factory;
    private final XBeeAddress64 xbeeAddress;
    
    private final Map<String, XBeeSensor> sensorMap = new HashMap<String, XBeeSensor>(); 
    private final Map<String, XBeeSwitch> switchMap = new HashMap<String, XBeeSwitch>(); 
    
    /**
     * Create an instance.
     * 
     * @param factory Device factory to refer back to.
     * @param xbeeAddress 64 bit XBee address.
     */
    public XBeeDeviceContainer(final XBeeDeviceFactory factory, final XBeeAddress64 xbeeAddress) {

        this.factory = factory;
        this.xbeeAddress = xbeeAddress;
    }
    
    @Override
    public SensorType getType() {
        
        return SensorType.PROTOTYPE;
    }
    
    @Override
    public String getAddress() {
        
        return Parser.render4x4(xbeeAddress);
    }
    
    public String toString() {
        
        StringBuilder sb = new StringBuilder();
        
        sb.append(getName()).append("@").append(getAddress());
        
        return sb.toString();
    }

    @Override
    public String getName() {
        
        return "XBee";
    }

    @Override
    public AnalogSensor getSensor(String address, SensorType type) {
        
        NDC.push("getSensor(" + address + ")");
        
        try {
        
            XBeeSensor s = sensorMap.get(address);

            if (s == null) {

                logger.debug("creating new instance");
                
                s = new XBeeSensor(this, address, type);
                sensorMap.put(address, s);
                
            } else {
                
                logger.debug("returning cached instance");
            }

            return s;
            
        } finally {
            NDC.pop();
        }
    }

    @Override
    public synchronized Switch getSwitch(String address) {
        
        NDC.push("getSwitch(" + address + ")");
        
        try {
        
            XBeeSwitch s = switchMap.get(address);

            if (s == null) {

                logger.debug("creating new instance");
                
                s = new XBeeSwitch(this, address);
                switchMap.put(address, s);
                
            } else {
                
                logger.debug("returning cached instance");
            }

            return s;
            
        } finally {
            NDC.pop();
        }
    }

    public XBeeResponse sendSynchronous(RemoteAtRequest request, int timeout) throws XBeeException {
        
        return factory.sendSynchronous(request, timeout);
    }

    public void broadcastIoSample(IoSample sample) {
        
        NDC.push("broadcastIoSample");
        
        long now = System.currentTimeMillis();
        
        try {
            
            for (Iterator<Entry<String, XBeeSensor>> i = sensorMap.entrySet().iterator(); i.hasNext(); ) {

                Entry<String, XBeeSensor> entry = i.next();
                String compositeAddress = entry.getKey();
                String channel = compositeAddress.substring(18);
                Double value = sample.getChannel(channel);
                
                logger.debug("channel: " + channel + "=" + value);
                
                if (value != null) {
                    
                    XBeeSensor sensor = entry.getValue();
                    
                    sensor.broadcast(now, value, null);
                }
            }
            
        } finally {
            NDC.pop();
        }
    }

    public void broadcastFailure(Throwable t) {
        
        NDC.push("broadcastFailure");
        
        long now = System.currentTimeMillis();
        
        try {

            for (Iterator<Entry<String, XBeeSensor>> i = sensorMap.entrySet().iterator(); i.hasNext(); ) {

                Entry<String, XBeeSensor> entry = i.next();
                XBeeSensor sensor = entry.getValue();

                sensor.broadcast(now, null, t);
            }
            
        } finally {
            NDC.pop();
        }
    }
}
