package net.sf.dz3.device.sensor.impl.xbee;

import java.util.Map;
import java.util.TreeMap;

import org.apache.logging.log4j.Logger;

import com.rapplogic.xbee.api.XBeeAddress64;

/**
 * XBee IO Sample.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko 2010-2018
 */
public class IoSample {

    /**
     * XBee ZB hardware ADC resolution.
     * 
     * 0 corresponds to 0V, 0x3FF corresponds to 1.2V.
     */
    final static double resolution = 1024d / 1200d;
    
    final static int[] analogBit = new int[] { 0x01, 0x02, 0x04, 0x08, 0x40 };
    final static String[] analogChannel = new String[] { "A0", "A1", "A2", "A3", "%V" };
    
    /**
     * Indicates which digital IO lines have sampling enabled. Each bit corresponds to one
     * digital IO line on the module.
     * 
     * bit 0 = AD0/DIO0
     * bit 1 = AD1/DIO1
     * bit 2 = AD2/DIO2
     * bit 3 = AD3/DIO3
     * bit 4 = DIO4
     * bit 5 = ASSOC/DIO5
     * bit 6 = RTS/DIO6
     * bit 7 = CTS/GPIO7
     * bit 8 = N/A
     * bit 9 = N/A
     * bit 10 = RSSI/DIO10
     * bit 11 = PWM/DIO11
     * bit 12 = CD/DIO12
     */
    public final int digitalMask;
    
    /**
     * Indicates which lines have analog inputs enabled for sampling. Each bit in the analog
     * channel mask corresponds to one analog input channel.
     * 
     * bit 0 = AD0/DIO0
     * bit 0 = AD1/DIO1
     * bit 0 = AD2/DIO2
     * bit 0 = AD3/DIO3
     * bit 7 = Supply Voltage
     */
    public final int analogMask;
    
    /**
     * Parsed bits of digital inputs.
     * 
     * {@code null} if no digital bits were present in {@link #digitalMask}.
     */
    public final Integer digital;
    
    /**
     * Value read from AD0. {@code null} if the bit is not set in {@link #analogMask}.
     */
    public final Double analog0;
    
    /**
     * Value read from AD1. {@code null} if the bit is not set in {@link #analogMask}.
     */
    public final Double analog1;
    
    /**
     * Value read from AD2. {@code null} if the bit is not set in {@link #analogMask}.
     */
    public final Double analog2;
    
    /**
     * Value read from AD3. {@code null} if the bit is not set in {@link #analogMask}.
     */
    public final Double analog3;
    
    /**
     * Value read from AD0. {@code null} if the supply voltage is within limits set by
     * {@code V+} command (hence, the bit is not set in {@link #analogMask}).
     */
    public final Double supplyVoltage;
    
    /**
     * Signal map.
     * 
     * The key is a channel value (corresponding to AT commands), the value is the value.
     * For analog signals it's the voltage in mV, for digital it is {@code 0d} for false
     * or {@code 1d} for true.
     * 
     * @see #parse()
     */
    private Map<String, Double> signalMap = new TreeMap<String, Double>();
    
    /**
     * Create an instance.
     * 
     * @param buffer XBee IO sample buffer (including the 0x01 leading "sample sets" byte).
     * @param xbeeAddress Address of XBee device the sample is being created for.
     */
    public IoSample(int[] buffer, XBeeAddress64 xbeeAddress, Logger logger) {
        
        if (buffer[0] != 0x01) {
            throw new IllegalArgumentException("Sample sets is not 0x01 (but " 
                    + Integer.toHexString(buffer[0]) + "), stream synchronization lost?");
        }
        
        digitalMask = buffer[1] << 8 | buffer[2];
        analogMask = buffer[3];
        
        int offset = 4;
        
        if (digitalMask == 0x0000) {
            
            digital = null;
            
        } else {
        
            digital = buffer[offset++] << 8 | buffer[offset];  
        }
        
        final Double[] sample = new Double[5];
        
        for (int bitOffset = 0; bitOffset < 5; bitOffset++) {
            
            int mask = analogMask & analogBit[bitOffset];
            
            if (mask != 0) {
                
                int msb = buffer[offset++];
                int lsb = buffer[offset++];
                int raw = msb << 8 | lsb;
                double rawDouble = (double) raw;
                
                if ((raw & ~0x3FF) == 0) {
                    
                    sample[bitOffset] = Double.valueOf(rawDouble / resolution);

                } else {
                    
                    logger.error("ADC sample out of range for " + Parser.render4x4(xbeeAddress) + ":"
                            + bitOffset + " (0x" + Integer.toHexString(raw)
                            + "), sample would've been " + Double.valueOf(rawDouble / resolution));
                    
                    sample[bitOffset] = null;

                }
                
                signalMap.put(analogChannel[bitOffset], sample[bitOffset]);
            }
        }
        
        analog0 = sample[0];
        analog1 = sample[1];
        analog2 = sample[2];
        analog3 = sample[3];
        supplyVoltage = sample[4];
    }
    
    /**
     * Return the channel value.
     * 
     * @param channel See {@link #signalMap}.
     * 
     * @return Channel value as {@code Double} or {@code null} if there is none in the sample.
     */
    public Double getChannel(String channel) {
        
        return signalMap.get(channel);
    }
    
    public String toString() {
        
        StringBuilder sb = new StringBuilder();
        
        // VT: FIXME: Add the rest, printed nicely
        
        sb.append("analog0=").append(analog0).append(",");
        sb.append("analog1=").append(analog1).append(",");
        sb.append("analog2=").append(analog2).append(",");
        sb.append("analog3=").append(analog3).append(",");
        sb.append("voltage=").append(supplyVoltage);
        
        return sb.toString();
    }
}
