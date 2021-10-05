package net.sf.dz3r.common;

/**
 * One channel hardware device address translated from hardware independent form.
 *
 * Useful for cases where a device with a single hardware address has several independently
 * addressable channels.
 *
 * @param <T> Channel address type.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2021
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
     * @param compositeAddress Hardware device address, followed by a colon, followed by the channel address.
     *
     * @exception IllegalArgumentException if the format can't be parsed.
     */
    protected ChannelAddress(String compositeAddress) {

        String[] parts = compositeAddress.split(":");

        if (parts.length < 2) {
            throw new IllegalArgumentException("Channel not present (separator is ':', remember?)");
        }

        if (parts.length > 2) {
            throw new IllegalArgumentException("Too many parts (separator is ':', remember?)");
        }

        hardwareAddress = parts[0];
        channel = parseChannel(parts[1]);

        if ("".equals(hardwareAddress) || channel == null) {
            throw new IllegalArgumentException("'" + compositeAddress + "' yields empty hardware and/or channel address");
        }
    }

    @Override
    public int compareTo(ChannelAddress<T> o) {
        return toString().compareTo(o.toString());
    }

    @Override
    public boolean equals(Object other) {

        return other instanceof ChannelAddress
                && hardwareAddress.equals(((ChannelAddress<?>) other).hardwareAddress)
                && channel.equals(((ChannelAddress<?>) other).channel);
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public final String toString() {
        return hardwareAddress + ':' + channel;
    }

    protected abstract T parseChannel(String rawChannel);
}
