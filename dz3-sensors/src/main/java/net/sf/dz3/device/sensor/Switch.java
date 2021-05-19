package net.sf.dz3.device.sensor;

import java.io.IOException;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSource;
import com.homeclimatecontrol.jukebox.jmx.JmxAttribute;
import com.homeclimatecontrol.jukebox.jmx.JmxAware;

/**
 * Dumb single channel switch.
 * 
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2019
 */
public interface Switch extends DataSource<Boolean>, JmxAware, Addressable {

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
