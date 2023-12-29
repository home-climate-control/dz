package net.sf.dz3r.device;

/**
 * An addressable entity.
 *
 * @param <T> Address type.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com"> Vadim Tkachenko 2000-2021
 */
public interface Addressable<T extends Comparable<T>> extends Comparable<Addressable<T>> {

    /**
     * Get the address.
     *
     * @return The address.
     */
    T getAddress();

    @Override
    default int compareTo(Addressable<T> other) {
        return getAddress().compareTo(other.getAddress());
    }
}
