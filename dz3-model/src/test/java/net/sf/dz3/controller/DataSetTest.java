package net.sf.dz3.controller;

import net.sf.dz3.instrumentation.Marker;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class DataSetTest {

    @Test
    public void testNonexistentValue() {
        
        DataSet<Double> ds = new DataSet<>(100);

        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> {
                    // Any value will fail, the set is empty
                    ds.get(500);
                })
                .withMessage("No value for time 500");
    }

    @Test
    public void testStrict() {
        
        DataSet<Double> ds = new DataSet<>(100, true);
        
        // Record values in order
        
        ds.record(100, 0d);
        ds.record(101, 0d);
        
        // We're fine so far

        assertThatIllegalArgumentException()
                .isThrownBy(() -> {
                    // This should blow up - this timestamp is out of order
                    ds.record(99, 0d);
                })
                .withMessage("Data element out of sequence: last key is 101, key being added is 99");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> {
                    // This also should blow up - this timestamp is already present
                    ds.record(101, 0d);
                })
                .withMessage("Data element out of sequence: last key is 101, key being added is 101");
    }

    @Test
    public void testExpire() {
        
        DataSet<Double> ds = new DataSet<Double>(100);
        
        ds.record(0, 0d);
        ds.record(100, 0d);

        assertThat(ds.iterator().next()).isZero();
        assertThat(ds.size()).isEqualTo(2);

        {
            
            // This value won't cause expiration
            ds.record(100, 0d);
            assertThat(ds.iterator().next()).isZero();
            assertThat(ds.size()).isEqualTo(2);
        }

        {
            // This value *will* cause expiration
            ds.record(101, 0d);
            assertThat(ds.iterator().next()).isEqualTo(100);
            assertThat(ds.size()).isEqualTo(2);
        }
    }


    @Test
    public void testPerformance10000000_100() {
        
        // This test completes roughly in 1.5s on the development system (with TreeSet based DataSet)
        // This test completes roughly in 850ms on the development system (with LinkedHashMap based DataSet)
        testPerformance(10000000, 100);
    }

    @Test
    public void testPerformance10000000_10000() {

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
