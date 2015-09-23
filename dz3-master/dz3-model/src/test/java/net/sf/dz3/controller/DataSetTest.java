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
}
