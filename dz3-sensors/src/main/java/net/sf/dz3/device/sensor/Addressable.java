package net.sf.dz3.device.sensor;

import com.homeclimatecontrol.jukebox.jmx.JmxAttribute;

/**
 * An object that has an address.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2021
 */
public interface Addressable extends Comparable<Addressable> {

    /**
     * Get the device address.
     *
     * The address is implementation dependent. The format of the address
     * string may differ depending on whether it is local or remote, what
     * kind of hardware it is, and so on and so forth.
     *
     * @return Device address.
     */
    @JmxAttribute(description = "Sensor address")
    String getAddress();
}
