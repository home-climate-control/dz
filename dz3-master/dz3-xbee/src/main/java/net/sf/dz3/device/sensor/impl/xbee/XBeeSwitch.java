package net.sf.dz3.device.sensor.impl.xbee;

import java.io.IOException;

import net.sf.dz3.device.sensor.Switch;
import net.sf.dz3.device.sensor.impl.StringChannelAddress;
import net.sf.dz3.instrumentation.Marker;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import com.rapplogic.xbee.api.AtCommandResponse;
import com.rapplogic.xbee.api.RemoteAtRequest;
import com.rapplogic.xbee.api.XBeeAddress64;

/**
 * XBee switch container.
 * 
 * Currently, this container is hardcoded to support the relay shield
 * at http://www.seeedstudio.com/depot/relay-shield-p-641.html,
 * but support will be soon extended to all XBee pins that can be configured as
 * digital outputs.
 *   
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko 2001-2010
 */
public class XBeeSwitch implements Switch {
    
    private final Logger logger = Logger.getLogger(getClass());
    
    private final XBeeDeviceContainer container;
    private StringChannelAddress address;
    
    /**
     * Create an instance.
     * 
     * @param container XBee device container to communicate through.
     * @param address Switch address.
     */
    public XBeeSwitch(final XBeeDeviceContainer container, final String address) {
        
        this.container = container;
        this.address = new StringChannelAddress(address);
    }

    @Override
    public boolean getState() throws IOException {

        NDC.push("read(" + address + ")");
        Marker m = new Marker("read(" + address + ")");

        try {
            
            XBeeAddress64 xbeeAddress = Parser.parse(address.hardwareAddress);
            String channel = address.channel;
            
            RemoteAtRequest request = new RemoteAtRequest(xbeeAddress, channel);
            AtCommandResponse rsp = (AtCommandResponse) container.sendSynchronous(request, XBeeConstants.TIMEOUT_AT_MILLIS);

            logger.info(channel + " response: " + rsp);

            if (rsp.isError()) {
                
                throw new IOException(channel + " + query failed, status: " + rsp.getStatus());
            }
            
            int buffer[] = rsp.getValue();
            
            if (buffer.length != 1) {
                
                throw new IOException("Unexpected buffer size " + buffer.length);
            }
            
            switch (buffer[0]) {
            case 4:
                
                return false;
                
            case 5:
                
                return true;
                
            default:
                
                throw new IOException(channel + " is not configured as switch, state is " + buffer[0]);
            }

        } catch (Throwable t) {

            IOException secondary = new IOException("Unable to read " + address);

            secondary.initCause(t);

            throw secondary;

        } finally {

            m.close();
            NDC.pop();
        }
    }

    @Override
    public void setState(boolean state) throws IOException {

        NDC.push("write(" + address + ")");
        Marker m = new Marker("write(" + address + ")");
        
        try {
            
            XBeeAddress64 xbeeAddress = Parser.parse(address.hardwareAddress);
            String channel = address.channel;
            
            int deviceState = state ? 5 : 4;
            RemoteAtRequest request = new RemoteAtRequest(xbeeAddress, channel, new int[] {deviceState});
            AtCommandResponse rsp = (AtCommandResponse) container.sendSynchronous(request, XBeeConstants.TIMEOUT_AT_MILLIS);

            logger.info(channel + " response: " + rsp);

            if (rsp.isError()) {
                
                throw new IOException(channel + " + query failed, status: " + rsp.getStatus());
            }

        } catch (Throwable t) {

            IOException secondary = new IOException("Unable to write " + address);

            secondary.initCause(t);

            throw secondary;

        } finally {

            m.close();
            NDC.pop();
        }
    }

    @Override
    public String getAddress() {

        return address.toString();
    }
}
