package net.sf.dz3.device.sensor.impl;

/**
 * One channel hardware switch address translated from hardware independent form
 * with the channel identifier being an non-negative integer.
 *   
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2010
 */
public class IntegerChannelAddress extends ChannelAddress<Integer> {
    
    /**
     * Create an instance out of a colon separated string.
     * 
     * @param address Hardware device address, followed by a colon, followed by decimal channel number.
     * 
     * @exception IllegalArgumentException if the format can't be parsed.
     */
    public IntegerChannelAddress(String address) {
        
        super(address);
        
    }

    @Override
    protected Integer parseChannel(String rawChannel) {
        
        int channel = Integer.parseInt(rawChannel);
        
        if (channel < 0) {
            throw new IllegalArgumentException("Channel number is non-negative (" + channel + " given)");
        }
        
        return channel;
    }
}
