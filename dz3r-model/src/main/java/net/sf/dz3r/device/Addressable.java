package net.sf.dz3r.device;

import com.homeclimatecontrol.jukebox.jmx.JmxAttribute;

/**
 * An addressable entity.
 *
 * @param <T> Address type.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com"> Vadim Tkachenko 2000-2021
 */
public interface Addressable<T extends Comparable<T>> extends Comparable<Addressable<T>> {

    /**
     * @return The address.
     */
    @JmxAttribute(description = "Address")
    T getAddress();

    default int compareTo(Addressable<T> other) {
        return getAddress().compareTo(other.getAddress());
    }
}