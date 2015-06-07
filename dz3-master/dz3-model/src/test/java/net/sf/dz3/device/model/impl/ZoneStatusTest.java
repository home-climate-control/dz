package net.sf.dz3.device.model.impl;

import java.util.Random;

import junit.framework.TestCase;

public class ZoneStatusTest extends TestCase {

    private final Random rg = new Random();
    
    public void testEqualsDouble() {
    
        for (int count = 0; count < 1000; count++) {

            double setpoint = rg.nextDouble();
            double delta = rg.nextDouble() * 0.00000000000000001d;
            
            ZoneStatusImpl status1 = new ZoneStatusImpl(setpoint, 0, true, true);
            ZoneStatusImpl status2 = new ZoneStatusImpl(setpoint + delta, 0, true, true);
            
            assertNotSame("Double comparison failure", status1, status2);
        }
    }
}
