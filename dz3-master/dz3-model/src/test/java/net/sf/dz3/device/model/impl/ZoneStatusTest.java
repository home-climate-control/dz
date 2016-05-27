package net.sf.dz3.device.model.impl;

import java.util.Random;

import net.sf.dz3.device.model.ZoneStatus;
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
    
    public void testSetpointNaN() {

        try {
        
            new ZoneStatusImpl(Double.NaN, 0, true, true);
            fail("Should've failed by now");
        
        } catch (IllegalArgumentException ex) {
            assertEquals("wrong exception message", "Invalid setpoint NaN", ex.getMessage());
        }
    }

    public void testSetpointPositiveInfinity() {

        try {
        
            new ZoneStatusImpl(Double.POSITIVE_INFINITY, 0, true, true);
            fail("Should've failed by now");
        
        } catch (IllegalArgumentException ex) {
            assertEquals("wrong exception message", "Invalid setpoint Infinity", ex.getMessage());
        }
    }

    public void testSetpointNegativeInfinity() {

        try {
        
            new ZoneStatusImpl(Double.NEGATIVE_INFINITY, 0, true, true);
            fail("Should've failed by now");
        
        } catch (IllegalArgumentException ex) {
            assertEquals("wrong exception message", "Invalid setpoint -Infinity", ex.getMessage());
        }
    }

    public void testNegativeDumpPriority() {

        try {
        
            new ZoneStatusImpl(0, -1, true, true);
            fail("Should've failed by now");
        
        } catch (IllegalArgumentException ex) {
            assertEquals("wrong exception message", "Dump priority must be non-negative (-1 given)", ex.getMessage());
        }
    }

    public void testEqualsNull() {

        assertFalse("Improper null comparison", new ZoneStatusImpl(0, 0, true, true).equals(null));
    }

    public void testEqualsAlien() {

        ZoneStatus zs = new ZoneStatusImpl(0, 0, true, true);

        assertFalse("Improper null comparison", zs.equals(zs.toString()));
    }

    public void testEqualsThermostatStatus() {

        ZoneStatus a = new ZoneStatusImpl(0, 0, true, true);
        ZoneStatus b = new ThermostatStatusImpl(0, 0, 0, true, true, true, false);

        assertTrue("Improper heterogenous comparison", a.equals(b));
    }

    public void testEqualsSame() {

        ZoneStatus a = new ZoneStatusImpl(0, 0, true, true);
        ZoneStatus b = new ZoneStatusImpl(0, 0, true, true);

        assertTrue("Improper comparison", a.equals(b));
    }

    public void testDifferentSetpoint() {

        ZoneStatus a = new ZoneStatusImpl(0, 0, true, true);
        ZoneStatus b = new ZoneStatusImpl(1, 0, true, true);

        assertFalse("Improper comparison", a.equals(b));
    }

    public void testDifferentDump() {

        ZoneStatus a = new ZoneStatusImpl(0, 0, true, true);
        ZoneStatus b = new ZoneStatusImpl(0, 1, true, true);

        assertFalse("Improper comparison", a.equals(b));
    }

    public void testDifferentEnabled() {

        ZoneStatus a = new ZoneStatusImpl(0, 0, true, true);
        ZoneStatus b = new ZoneStatusImpl(0, 0, false, true);

        assertFalse("Improper comparison", a.equals(b));
    }

    public void testDifferentVoting() {

        ZoneStatus a = new ZoneStatusImpl(0, 0, true, true);
        ZoneStatus b = new ZoneStatusImpl(0, 0, true, false);

        assertFalse("Improper comparison", a.equals(b));
    }

    public void testHashCodeEquals() {

        ZoneStatus a = new ZoneStatusImpl(0, 0, true, true);
        ZoneStatus b = new ZoneStatusImpl(0, 0, true, true);

        assertTrue("Improper hashcode comparison", a.hashCode() == b.hashCode());
    }

    public void testHashCodeDiffers() {

        ZoneStatus a = new ZoneStatusImpl(0, 0, true, true);
        ZoneStatus b = new ZoneStatusImpl(1, 0, true, true);

        assertFalse("Improper hashcode comparison", a.hashCode() == b.hashCode());
    }

    public void testToString0011() {

        assertEquals("Wrong string representation", new ZoneStatusImpl(0, 0, true, true).toString(), "setpoint=0.0, enabled, voting");
    }

    public void testToString0111() {

        assertEquals("Wrong string representation", new ZoneStatusImpl(0, 1, true, true).toString(), "setpoint=0.0, enabled, voting, dump priority=1");
    }

    public void testToString0101() {

        assertEquals("Wrong string representation", new ZoneStatusImpl(0, 1, false, true).toString(), "setpoint=0.0, disabled, voting, dump priority=1");
    }

    public void testToString0110() {

        assertEquals("Wrong string representation", new ZoneStatusImpl(0, 1, true, false).toString(), "setpoint=0.0, enabled, not voting, dump priority=1");
    }
}
