package net.sf.dz3.controller;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import net.sf.dz3.controller.pid.DifferentialSet;
import net.sf.dz3.controller.pid.LegacyDifferentialSet;
import net.sf.dz3.controller.pid.NaiveDifferentialSet;
import net.sf.dz3.controller.pid.SlidingDifferentialSet;
import net.sf.dz3.instrumentation.Marker;

/**
 * @author <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2018
 */
public class DifferentialSetTest extends TestCase {

    private final Logger logger = LogManager.getLogger(getClass());

    private final Random rg = new Random();
    private Semaphore startGate = new Semaphore(3);
    private Semaphore stopGate = new Semaphore(3);

    private static final long INTEGRATION_INTERVAL = 10000L;
    private static final int COUNT = 100000;
    private static final int TICK = 100;

    /**
     * Compare slow and fast implementation speed.
     *
     * This test doesn't test the implementation correctness.
     */
    public void testAll() throws InterruptedException {

        startGate.acquire(3);

        Thread t1 = new Thread(new Legacy(INTEGRATION_INTERVAL));
        Thread t2 = new Thread(new Naive(INTEGRATION_INTERVAL));
        Thread t3 = new Thread(new Sliding(INTEGRATION_INTERVAL));

        t1.start();
        t2.start();
        t3.start();

        startGate.release(3);
        logger.info("unleashed");

        stopGate.acquire(3);

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
     * Make sure the slow and fast implementation yield the same results, triggering expiration.
     */
    public void DISABLED_testSameWithExpiration() {

        int count = 10000;

        // Make sure the expiration interval is within the possible timestamp advance. Statistically.
        long expirationInterval = (count/10) * TICK;

        testSame(count, expirationInterval);
    }

    /**
     * Make sure the slow and fast implementation yield the same results.
     */
    public void testSame(int limit, long expirationInterval) {

        ThreadContext.push("testSame/D(" + limit + ", " + expirationInterval + ")");

        Marker m = new Marker("testSame");
        int count = 0;

        Long lastGoodTimestamp = null;

        try {

            DifferentialSet dataSet2000 = new LegacyDifferentialSet(expirationInterval);
            DifferentialSet dataSet2015 = new NaiveDifferentialSet(expirationInterval);
            DifferentialSet dataSetFast = new SlidingDifferentialSet(expirationInterval);

            long timestamp = 0;

            for ( ; count < limit; count++) {

                timestamp += Math.abs(rg.nextInt(TICK)) + 1;
                double value = rg.nextDouble();

                dataSet2000.record(timestamp, value);
                dataSet2015.record(timestamp, value);
                dataSetFast.record(timestamp, value);

                assertEquals("2000/2015 differ", dataSet2000.getDifferential(), dataSet2015.getDifferential(), 0.0001);
                assertEquals("2015/slide differ", dataSet2015.getDifferential(), dataSetFast.getDifferential(), 0.0001);

                lastGoodTimestamp = timestamp;
            }

        } finally {

            if (count < limit) {
                logger.info("Survived " + count + "/" + limit + " iterations, last good timestamp is " + lastGoodTimestamp);
            }

            m.close();
            ThreadContext.pop();
        }

    }

    /**
     * Make sure the slow and fast implementation yield the same results, step by step, with NO more than one record ever expired.
     */
    public void testSameSingleExpiration80() {

        List<Long> timestamps = new LinkedList<Long>();

        // Make sure no intervals exceed the expiration interval so no more than one entry ever needs to be expired
        timestamps.add(80L);
        timestamps.add(80L);
        timestamps.add(80L);
        timestamps.add(80L);
        timestamps.add(80L);
        timestamps.add(80L);

        testSameSteps("single-80", 100, timestamps);
    }

    /**
     * Make sure the slow and fast implementation yield the same results, step by step, with NO more than one record ever expired.
     */
    public void testSameSingleExpiration50() {

        List<Long> timestamps = new LinkedList<Long>();

        // Make sure no intervals exceed the expiration interval so no more than one entry ever needs to be expired
        timestamps.add(50L);
        timestamps.add(50L);
        timestamps.add(50L);
        timestamps.add(50L);
        timestamps.add(50L);
        timestamps.add(50L);

        testSameSteps("single-50", 100, timestamps);
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

        testSameSteps("multiple", 100, timestamps);
    }

    /**
     * Make sure the slow and fast implementation yield the same results, step by step, with MORE than one record ever expired.
     */
    public void testSameFirstExpiration() {

        List<Long> timestamps = new LinkedList<Long>();

        // The first item added will trigger the expiration already
        timestamps.add(200L);
        timestamps.add(50L);
        timestamps.add(50L);
        timestamps.add(50L);

        testSameSteps("first", 100, timestamps);
    }

    /**
     * Make sure the slow and fast implementation yield the same results.
     */
    public void testSameSteps(String marker, long expirationInterval, List<Long> timestamps) {

        ThreadContext.push("testSameSteps/D-" + marker);

        try {

            DifferentialSet dataSet2000 = new LegacyDifferentialSet(expirationInterval);
            DifferentialSet dataSet2015 = new NaiveDifferentialSet(expirationInterval);
            DifferentialSet dataSetFast = new SlidingDifferentialSet(expirationInterval);

            long timestamp = 0;
            long count = 0;

            for (Iterator<Long> i = timestamps.iterator(); i.hasNext(); ) {

                ThreadContext.push("" + count++);

                try {

                    timestamp += i.next();
                    double value = rg.nextDouble();

                    dataSet2000.record(timestamp, value);
                    dataSet2015.record(timestamp, value);
                    dataSetFast.record(timestamp, value);

                    double i2000 = dataSet2000.getDifferential();
                    double i2015 = dataSet2015.getDifferential();
                    double iFast = dataSetFast.getDifferential();

                    logger.info("timestamp/value/expiration: " + timestamp + "/" + value + "/" + expirationInterval);
                    logger.debug("old/new/fast: " + i2000 + " " + i2015 + " " + iFast);

                    assertEquals("2000/2015 differ", i2000, i2015, 0.0001);
                    assertEquals("2015/slide differ", i2015, iFast, 0.0001);

                } finally {
                    ThreadContext.pop();
                }
            }

            logger.debug("Success");

        } catch (AssertionFailedError e) {

            logger.error("Failure");

            throw e;

        } finally {

            ThreadContext.pop();
        }

    }

    private abstract class Runner implements Runnable {

        protected final long expirationInterval;

        Runner(long expirationInterval) throws InterruptedException {

            this.expirationInterval = expirationInterval;

            stopGate.acquire();
        }

        @Override
        public void run() {

            DifferentialSet dataSet = createSet(expirationInterval);

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

        protected abstract DifferentialSet createSet(long expirationInterval);
        protected abstract void sample(DifferentialSet dataSet);
    }

    private class Naive extends Runner {

        Naive(long expirationInterval) throws InterruptedException {
            super(expirationInterval);
        }

        @Override
        protected DifferentialSet createSet(long expirationInterval) {
            return new NaiveDifferentialSet(expirationInterval);
        }

        @Override
        protected void sample(DifferentialSet dataSet) {
            dataSet.getDifferential();
        }
    }

    private class Legacy extends Runner {

        Legacy(long expirationInterval) throws InterruptedException {
            super(expirationInterval);
        }

        @Override
        protected DifferentialSet createSet(long expirationInterval) {
            return new LegacyDifferentialSet(expirationInterval);
        }

        @Override
        protected void sample(DifferentialSet dataSet) {
            dataSet.getDifferential();
        }
    }

    private class Sliding extends Runner {

        Sliding(long expirationInterval) throws InterruptedException {
            super(expirationInterval);
        }

        @Override
        protected DifferentialSet createSet(long expirationInterval) {
            return new SlidingDifferentialSet(expirationInterval);
        }

        @Override
        protected void sample(DifferentialSet dataSet) {
            dataSet.getDifferential();
        }
    }
}
