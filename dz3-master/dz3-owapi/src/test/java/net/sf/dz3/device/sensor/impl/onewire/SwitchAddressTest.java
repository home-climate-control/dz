package net.sf.dz3.device.sensor.impl.onewire;

import java.util.Set;
import java.util.TreeSet;

import junit.framework.TestCase;
import net.sf.dz3.device.sensor.impl.IntegerChannelAddress;
import net.sf.dz3.device.sensor.impl.StringChannelAddress;

/**
 * Set of test cases for {@link StringChannelAddress}.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko 2001-2009
 */
public class SwitchAddressTest extends TestCase {
    
    public void testGood() {
        
        @SuppressWarnings("unused")
        StringChannelAddress sa = new StringChannelAddress("1300000000E6B51F:1");
    }

    public void testNoChannel() {
        
        try {
        
            @SuppressWarnings("unused")
            StringChannelAddress sa = new StringChannelAddress("1300000000E6B51F");
            fail("Should've thrown an exception");
        
        } catch (IllegalArgumentException ex) {
            assertEquals("Wronge exception message", "Channel not present (separator is ':', remember?)", ex.getMessage());
        }
    }

    public void testNotDecimal() {
        
        try {
            
            @SuppressWarnings("unused")
            IntegerChannelAddress sa = new IntegerChannelAddress("1300000000E6B51F:0x0f");
            fail("Should've thrown an exception");
        
        } catch (NumberFormatException ex) {
            assertEquals("Wronge exception message", "For input string: \"0x0f\"", ex.getMessage());
        }
    }

    public void testTooManyParts() {
        
        try {
            
            @SuppressWarnings("unused")
            StringChannelAddress sa = new StringChannelAddress("1300000000E6B51F:1:2");
            fail("Should've thrown an exception");
        
        } catch (IllegalArgumentException ex) {
            assertEquals("Wronge exception message", "Too many parts (separator is ':', remember?)", ex.getMessage());
        }
    }

    public void testNegativeChannel() {
        
        try {
            
            @SuppressWarnings("unused")
            IntegerChannelAddress sa = new IntegerChannelAddress("1300000000E6B51F:-1");
            fail("Should've thrown an exception");
        
        } catch (IllegalArgumentException ex) {
            assertEquals("Wronge exception message", "Channel number is non-negative (-1 given)", ex.getMessage());
        }
    }
    
    public void testComparable() {
        
        StringChannelAddress s0 = new StringChannelAddress("1300000000E6B51F:0");
        StringChannelAddress s1 = new StringChannelAddress("1300000000E6B51F:1");
        
        assertTrue(s1.compareTo(s0) > 0);
        
        Set<StringChannelAddress> set = new TreeSet<StringChannelAddress>();
        
        set.add(s0);
        set.add(s1);
    }
}
