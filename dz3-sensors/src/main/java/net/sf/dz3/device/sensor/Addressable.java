package net.sf.dz3.device.sensor;

import net.sf.jukebox.jmx.JmxAttribute;

/**
 * An object that has an address.
 *  
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko 2001-2009
 */
public interface Addressable {

    /**
     * Get the sensor hardware address.
     *
     * The address is implementation dependent. The format of the address
     * string may differ depending on whether it is local or remote, what
     * kind of hardware it is, and so on and so forth.
     *
     * @return Hardware sensor address.
     */
    @JmxAttribute(description = "Sensor address")
    String getAddress();
}
