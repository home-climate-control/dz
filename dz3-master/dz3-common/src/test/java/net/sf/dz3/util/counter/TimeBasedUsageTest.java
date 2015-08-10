package net.sf.dz3.util.counter;

import junit.framework.TestCase;

public class TimeBasedUsageTest extends TestCase {
    
    public void testNormalOn() {
        
        TimeBasedUsage tbu = new TimeBasedUsage();
        
        long before = System.currentTimeMillis();
        long after = before + 10;

        tbu.consume(before, 1);
        
        assertEquals("Wrong consumed value", 10, tbu.consume(after, 1));
    }

    public void testNormalOff() {
        
        TimeBasedUsage tbu = new TimeBasedUsage();
        
        long before = System.currentTimeMillis();
        long after = before + 10;

        tbu.consume(before, 0);
        
        assertEquals("Wrong consumed value", 0, tbu.consume(after, 0));
    }

    public void testBackInTime() {
        
        TimeBasedUsage tbu = new TimeBasedUsage();
        
        long before = System.currentTimeMillis();
        long after = before + 10;

        tbu.consume(after, 1);
        
        assertEquals("Wrong consumed value", 0, tbu.consume(before, 1));
    }
}
