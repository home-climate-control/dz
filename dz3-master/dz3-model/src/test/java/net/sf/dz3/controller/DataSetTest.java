package net.sf.dz3.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.NoSuchElementException;
import java.util.Random;

import org.junit.Test;

import net.sf.dz3.instrumentation.Marker;

public class DataSetTest {

    @Test
    public void merge() {

        DataSet<Double> ds = new DataSet<Double>(100);

        ds.record(0, 1.0);
        ds.record(1, 1.0);

        assertEquals("wrong size", 2, ds.size());

        ds.record(2, 1.0, true);

        assertEquals("wrong data set size", 2, ds.size());
    }

    /**
     * Make sure that the value that would've been merged if not expired is correctly expired.
     */
    @Test
    public void mergeExpire() {

        DataSet<Double> ds = new DataSet<Double>(100);

        ds.record(1, 1.0);

        assertEquals("wrong data set size", 1, ds.size());

        ds.record(2, 1.0, true);

        assertEquals("wrong data set size", 1, ds.size());

        // This one blows the old one past expiration

        ds.record(200, 1.0, true);

        assertEquals("Wrong value after expiration", 200, ds.iterator().next().longValue());
        assertEquals("Wrong data set size", 1, ds.size());
    }

    @Test
    public void nonexistentValue() {

        DataSet<Double> ds = new DataSet<Double>(100);

        try {

            // Any value will fail, the set is empty
            ds.get(500);

            fail("Should've failed by now");

        } catch (NoSuchElementException ex) {

            assertEquals("Wrong exception message", "No value for time 500", ex.getMessage());
        }
    }

    @Test
    public void strict() {

        DataSet<Double> ds = new DataSet<Double>(100, true);

        // Record values in order

        ds.record(100, 0d);
        ds.record(101, 0d);

        // We're fine so far

        try {

            // This should blow up - this timestamp is out of order
            ds.record(99, 0d);

            fail("Should've failed by now");

        } catch (IllegalArgumentException ex) {
            assertEquals("Wrong exception message", "Data element out of sequence: last key is 101, key being added is 99", ex.getMessage());
        }

        try {

            // This also should blow up - this timestamp is already present
            ds.record(101, 0d);

            fail("Should've failed by now");

        } catch (IllegalArgumentException ex) {
            assertEquals("Wrong exception message", "Data element out of sequence: last key is 101, key being added is 101", ex.getMessage());
        }
    }

    @Test
    public void expire() {

        DataSet<Double> ds = new DataSet<Double>(100);

        ds.record(0, 0d);
        ds.record(100, 0d);

        assertEquals("Wrong value", 0, ds.iterator().next().longValue());
        assertEquals("Wrong data set size", 2, ds.size());

        {
            // This value won't cause expiration
            ds.record(100, 0d);
            assertEquals("Wrong value before expiration", 0, ds.iterator().next().longValue());
            assertEquals("Wrong data set size", 2, ds.size());
        }

        {
            // This value *will* cause expiration
            ds.record(101, 0d);
            assertEquals("Wrong value after expiration", 100, ds.iterator().next().longValue());
            assertEquals("Wrong data set size", 2, ds.size());
        }
    }

    @Test
    public void performance10000000_100() {

        // This test completes roughly in 1.5s on the development system (with TreeSet based DataSet)
        // This test completes roughly in 850ms on the development system (with LinkedHashMap based DataSet)
        testPerformance(10000000, 100);
    }

    @Test
    public void performance10000000_10000() {

        // This test completes roughly in 2.5s on the development system (with TreeSet based DataSet)
        // This test completes roughly in 850ms on the development system (with LinkedHashMap based DataSet)
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
