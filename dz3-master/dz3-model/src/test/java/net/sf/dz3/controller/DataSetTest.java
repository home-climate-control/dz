package net.sf.dz3.controller;

import java.util.Random;

import net.sf.dz3.instrumentation.Marker;
import junit.framework.TestCase;

public class DataSetTest extends TestCase {

    public void testStrict() {
        
        DataSet<Double> ds = new DataSet<Double>(100);
        
        ds.setStrict(true);
        
        // Record values in order
        
        ds.record(100, 0d);
        ds.record(101, 0d);
        
        // We're fine so far

        try {
            
            // This should blow up
            ds.record(99, 0d);
            
            fail("Should've failed by now");
            
        } catch (IllegalArgumentException ex) {
            assertEquals("Wrong exception message", "Data element out of sequence: last key is 101, key being added is 99", ex.getMessage());
        }
    }

    public void testExpire() {
        
        DataSet<Double> ds = new DataSet<Double>(100);
        
        ds.record(0, 0d);
        ds.record(100, 0d);
        
        assertEquals("Wrong value", 0, ds.iterator().next().longValue());

        {
            
            // This value won't cause expiration
            ds.record(100, 0d);
            assertEquals("Wrong value before expiration", 0, ds.iterator().next().longValue());
        }

        {
            // This value *will* cause expiration
            ds.record(101, 0d);
            assertEquals("Wrong value after expiration", 100, ds.iterator().next().longValue());
        }
    }
    
    public void testPerformance10000000_100() {
        
        // This test completes roughly in 1.5s on the development system (with TreeSet based DataSet)
        testPerformance(10000000, 100);
    }
    
    public void testPerformance10000000_10000() {

        // This test completes roughly in 2.5s on the development system (with TreeSet based DataSet)
        testPerformance(10000000, 10000);
    }
    
    private void testPerformance(long entryCount, long expirationInterval) {
        
        DataSet<Double> ds = new DataSet<Double>(expirationInterval);
        long timestamp = 0;
        Random rg = new Random();
        
        Marker m = new Marker("testPerformance(" + entryCount + ", " + expirationInterval + ")");

        for (int count = 0; count < entryCount; count++) {
            
            timestamp += rg.nextInt(10);
            
            ds.record(timestamp, rg.nextDouble());
        }
        
        m.close();
    }
}
