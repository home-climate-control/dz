package net.sf.dz3.device.sensor;

import java.io.IOException;


/**
 * Switch container abstraction.
 * 
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2000-2010
 */
public interface DzSwitchContainer extends DeviceContainer {

    /**
     * Get the number of channels this switch provides.
     * 
     * @return number of channels.
     */
    int getChannelCount();

    /**
     * Write value.
     * 
     * @param channel Channel to write to.
     * @param value Value to write.
     * @exception IOException if there was a problem talking to the device.
     */
    void write(int channel, boolean value) throws IOException;

    /**
     * Read channel value.
     * 
     * @param channel Channel to read.
     * @return Value for the given channel.
     * @exception IOException if there was a problem talking to the device.
     */
    boolean read(int channel) throws IOException;

    /**
     * Reset the switch to default state (all zeros).
     * 
     * @exception IOException if there was a problem talking to the device.
     */
    void reset() throws IOException;
}
