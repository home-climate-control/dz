package net.sf.dz3.device.sensor.impl;

import java.io.IOException;

import org.apache.log4j.Logger;

import net.sf.dz3.device.sensor.Switch;
import net.sf.jukebox.jmx.JmxAware;
import net.sf.jukebox.jmx.JmxDescriptor;

/**
 * Null switch.
 * 
 * Does absolutely nothing other than reflecting itself in the log and via JMX.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko 2001-2010
 */
public class NullSwitch implements Switch, JmxAware {
    
    private final Logger logger = Logger.getLogger(getClass());
    
    /**
     * Switch address.
     */
    private final String address;
    
    /**
     * Switch state.
     */
    private boolean state = false;

    /**
     * Create an instance.
     * 
     * @param address Address to use.
     */
    public NullSwitch(String address) {
        
        this.address = address;
    }
    
    @Override
    public boolean getState() throws IOException {
        
        return state;
    }

    @Override
    public void setState(boolean state) throws IOException {

        logger.debug("Switch " + address + "=" + state);
        this.state = state;
    }

    @Override
    public String getAddress() {
        
        return address;
    }

    @Override
    public JmxDescriptor getJmxDescriptor() {

        return new JmxDescriptor(
                "dz",
                "Null Switch",
                Integer.toHexString(hashCode()),
                "Pretends to be the actual switch");
    }
}
