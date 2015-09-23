package net.sf.dz3.controller;

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
}
