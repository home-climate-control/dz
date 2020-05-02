package net.sf.dz3.device.sensor.impl;

/**
 * One channel hardware device address translated from hardware independent form.
 *
 * Useful for cases where a device with a single hardware address has several independently
 * addressable channels.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko 2001-2010
 */
public abstract class ChannelAddress<T> implements Comparable<ChannelAddress<T>> {

    /**
     * Hardware address as a string.
     */
    public final String hardwareAddress;

    /**
     * Channel identifier.
     */
    public final T channel;

    /**
     * Create an instance out of a colon separated string.
     *
     * @param address Hardware device address, followed by a colon, followed by the channel address.
     *
     * @exception IllegalArgumentException if the format can't be parsed.
     */
    public ChannelAddress(String compositeAddress) {

        String[] parts = compositeAddress.split(":");

        if (parts.length < 2) {
            throw new IllegalArgumentException("Channel not present (separator is ':', remember?)");
        }

        if (parts.length > 2) {
            throw new IllegalArgumentException("Too many parts (separator is ':', remember?)");
        }

        // There's not much to check
        hardwareAddress = parts[0];
        channel = parseChannel(parts[1]);
    }

    @Override
    public int compareTo(ChannelAddress<T> o) {

        return toString().compareTo(o.toString());
    }

    @Override
    public boolean equals(Object other) {

        if (this == other) {
            return true;
        }

        if (other == null) {
            return false;
        }

        return toString().equals(other.toString());
    }

    @Override
    public int hashCode() {

        return toString().hashCode();
    }

    @Override
    public final String toString() {

        StringBuilder sb = new StringBuilder();

        sb.append(hardwareAddress).append(':').append(channel);

        return sb.toString();
    }


    protected abstract T parseChannel(String rawChannel);
}
