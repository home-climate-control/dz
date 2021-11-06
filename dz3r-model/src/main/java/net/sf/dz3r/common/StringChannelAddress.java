package net.sf.dz3r.common;

/**
 * One channel hardware switch address translated from hardware independent form,
 * with the channel identifier being a string.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2021
 */
public class StringChannelAddress extends ChannelAddress<String> {

    /**
     * Create an instance out of a colon separated string.
     *
     * @param address Hardware device address, followed by a colon, followed by string channel address.
     *
     * @exception IllegalArgumentException if the format can't be parsed.
     */
    public StringChannelAddress(String address) {
        super(address);
    }

    @Override
    protected String parseChannel(String rawChannel) {

        if (rawChannel == null || "".equals(rawChannel)) {
            throw new IllegalArgumentException("channel can't be null or empty");
        }

        return rawChannel;
    }
}
