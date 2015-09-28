package net.sf.dz3.controller;

import junit.framework.TestCase;
import net.sf.dz3.controller.pid.IntegralSet;
import net.sf.dz3.controller.pid.LegacyIntegralSet;
import net.sf.dz3.controller.pid.NaiveIntegralSet;
import net.sf.dz3.instrumentation.Marker;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;

/**
 * @author <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2015
 */
public class IntegralSetTest extends TestCase {

    private final Logger logger = Logger.getLogger(getClass());

    private final Random rg = new Random();
    private Semaphore startGate = new Semaphore(2);
    private Semaphore stopGate = new Semaphore(2);

    private static final long INTEGRATION_INTERVAL = 10000L;
    private static final int COUNT = 1000000;
    private static final int TICK = 100;

    /**
     * Compare slow and fast implementation speed.
     * 
     * This test doesn't test the implementation correctness.
     */
    public void testAll() throws InterruptedException {

        startGate.acquire(2);

        Thread t1 = new Thread(new Slow(INTEGRATION_INTERVAL));
        Thread t2 = new Thread(new Fast(INTEGRATION_INTERVAL));

        t1.start();
        t2.start();

        startGate.release(2);
        logger.info("unleashed");

        stopGate.acquire(2);

        logger.info("done");
    }
    
    /**
     * Make sure the slow and fast implementation yield the same results, without triggering expiration.
     */
    public void testSameNoExpiration() {
        
        int count = 100;
        
        // Make sure the expiration interval is beyond the possible timestamp advance
        long expirationInterval = (count + count/2) * TICK; 
                
        testSame(count, expirationInterval);
    }

    /**
     * Make sure the slow and fast implementation yield the same results, without triggering expiration.
     */
    public void testSameWithExpiration() {
        
        int count = 10000;
        
        // Make sure the expiration interval is within the possible timestamp advance. Statistically.
        long expirationInterval = (count/10) * TICK; 
                
        testSame(count, expirationInterval);
    }

    /**
     * Make sure the slow and fast implementation yield the same results.
     */
    public void testSame(int limit, long expirationInterval) {

        NDC.push("testSame/I(" + limit + ", " + expirationInterval + ")");

        Marker m = new Marker("testSame");
        int count = 0;
        
        try {

            IntegralSet dataSet2000 = new LegacyIntegralSet(expirationInterval);
            IntegralSet dataSet2015 = new NaiveIntegralSet(expirationInterval);

            long start = System.currentTimeMillis();
            long timestamp = start;

            for ( ; count < limit; count++) {

                timestamp += Math.abs(rg.nextInt(TICK)) + 1;
                double value = rg.nextDouble();

                dataSet2000.record(timestamp, value);
                dataSet2015.record(timestamp, value);

                assertEquals(dataSet2000.getIntegral(), dataSet2015.getIntegral());
            }

            long now = System.currentTimeMillis();

            logger.info((now - start) + "ms");

        } finally {
            
            if (count < limit) {
                logger.info("Survived " + count + "/" + limit + " iterations");
            }
        
            m.close();
            NDC.pop();
        }

    }

    /**
     * Make sure the slow and fast implementation yield the same results, step by step, with NO more than one record ever expired.
     */
    public void testSameSingleExpiration() {
        
        List<Long> timestamps = new LinkedList<Long>();

        // Make sure no intervals exceed the expiration interval so no more than one entry ever needs to be expired
        timestamps.add(80L);
        timestamps.add(80L);
        timestamps.add(80L);
        timestamps.add(80L);
        timestamps.add(80L);
        timestamps.add(80L);
        
        testSameSteps(100, timestamps);
    }

    /**
     * Make sure the slow and fast implementation yield the same results, step by step, with MORE than one record ever expired.
     */
    public void testSameMultipleExpiration() {
        
        List<Long> timestamps = new LinkedList<Long>();

        timestamps.add(80L);
        timestamps.add(80L);
        timestamps.add(80L);
        
        // This difference will trigger expiration for more than one record
        timestamps.add(300L);
        
        timestamps.add(80L);
        timestamps.add(80L);
        
        testSameSteps(100, timestamps);
    }

    /**
     * Make sure the slow and fast implementation yield the same results.
     */
    public void testSameSteps(long expirationInterval, List<Long> timestamps) {

        NDC.push("testSameSteps/I");

        try {

            IntegralSet dataSet2000 = new LegacyIntegralSet(expirationInterval);
            IntegralSet dataSet2015 = new NaiveIntegralSet(expirationInterval);

            long timestamp = 0;

            for (Iterator<Long> i = timestamps.iterator(); i.hasNext(); ) {

                timestamp += i.next();
                double value = rg.nextDouble();

                dataSet2000.record(timestamp, value);
                dataSet2015.record(timestamp, value);
                
                logger.info("timestamp/expiration: " + timestamp + "/" + expirationInterval);

                assertEquals(dataSet2000.getIntegral(), dataSet2015.getIntegral());
            }

        } finally {
            
            NDC.pop();
        }

    }

    private abstract class Runner implements Runnable {
        
        protected final long expirationInterval;
        
        Runner(long expirationInterval) throws InterruptedException {
            
            this.expirationInterval = expirationInterval;
            
            stopGate.acquire();
        }

        public void run() {

            IntegralSet dataSet = createSet(expirationInterval);

            try {
                startGate.acquire();
            } catch (InterruptedException e) {
                logger.info("Interrupted", e);
            }

            Marker m = new Marker("run/" + dataSet.getClass().getSimpleName());
            long timestamp = System.currentTimeMillis();

            for (int count = 0; count < COUNT; count++) {

                timestamp += Math.abs(rg.nextInt(TICK)) + 1;
                double value = rg.nextDouble();

                dataSet.record(timestamp, value);

                sample(dataSet);
            }

            m.close();
            stopGate.release();
        }

        protected abstract IntegralSet createSet(long expirationInterval);
        protected abstract void sample(IntegralSet dataSet);
    }

    private class Fast extends Runner {

        Fast(long expirationInterval) throws InterruptedException {
            super(expirationInterval);
        }

        @Override
        protected IntegralSet createSet(long expirationInterval) {
            return new NaiveIntegralSet(expirationInterval);
        }
        
        @Override
        protected void sample(IntegralSet dataSet) {
            dataSet.getIntegral();
        }
    }

    private class Slow extends Runner {

        Slow(long expirationInterval) throws InterruptedException {
            super(expirationInterval);
        }

        @Override
        protected IntegralSet createSet(long expirationInterval) {
            return new LegacyIntegralSet(expirationInterval);
        }

        @Override
        protected void sample(IntegralSet dataSet) {
            dataSet.getIntegral();
        }
    }
}
