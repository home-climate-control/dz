package net.sf.dz3.device.sensor;

import java.io.IOException;

import net.sf.jukebox.jmx.JmxAttribute;

/**
 * Dumb single channel switch.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko 2001-2009
 */
public interface Switch extends Addressable {

    /**
     * Get the switch state.
     * 
     * @return Switch state.
     */
    @JmxAttribute(description = "switch state")
    boolean getState() throws IOException;
    
    /**
     * Set the switch state.
     *  
     * @param state State to set.
     */
    void setState(boolean state) throws IOException;
    
}
