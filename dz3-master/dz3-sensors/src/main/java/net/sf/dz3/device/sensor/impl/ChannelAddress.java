package net.sf.dz3.device.sensor.impl;

/**
 * One channel hardware device address translated from hardware independent form.
 *
 * Useful for cases where a device with a single hardware address has several independently
 * addressable channels.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko 2001-2010
 */
public abstract class ChannelAddress<ChannelId> implements Comparable<ChannelAddress<ChannelId>> {

    /**
     * Hardware address as a string.
     */
    public final String hardwareAddress;

    /**
     * Channel identifier.
     */
    public final ChannelId channel;

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
    public int compareTo(ChannelAddress<ChannelId> o) {

        return toString().compareTo(o.toString());
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj) {

        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        ChannelAddress<ChannelId> other = (ChannelAddress<ChannelId>) obj;

        if (!channel.equals(other.channel)) {
            return false;
        }

        if (hardwareAddress == null) {
            if (other.hardwareAddress != null) {
                return false;
            }
        } else if (!hardwareAddress.equals(other.hardwareAddress)) {
            return false;
        }

        return true;
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


    protected abstract ChannelId parseChannel(String rawChannel);
}
