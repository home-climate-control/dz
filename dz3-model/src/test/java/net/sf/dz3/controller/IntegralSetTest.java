package net.sf.dz3.controller;

import net.sf.dz3.controller.pid.IntegralSet;
import net.sf.dz3.controller.pid.NaiveIntegralSet;
import net.sf.dz3.controller.pid.SlidingIntegralSet;
import net.sf.dz3.instrumentation.Marker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.within;

/**
 * @author <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2018
 */
class IntegralSetTest {

    private final Logger logger = LogManager.getLogger(getClass());

    private final Random rg = new Random();
    private final Semaphore startGate = new Semaphore(3);
    private final Semaphore stopGate = new Semaphore(3);

    private static final long INTEGRATION_INTERVAL = 10000L;
    private static final int COUNT = 1000000;
    private static final int TICK = 100;

    /**
     * Compare slow and fast implementation speed.
     *
     * This test doesn't test the implementation correctness.
     */
    @Test
    void all() throws InterruptedException {

        assertThatCode(() -> {
            startGate.acquire(3);

            Thread t2 = new Thread(new Naive(INTEGRATION_INTERVAL));
            Thread t3 = new Thread(new Sliding(INTEGRATION_INTERVAL));

            t2.start();
            t3.start();

            startGate.release(3);
            logger.info("unleashed");

            stopGate.acquire(3);

            logger.info("done");
        }).doesNotThrowAnyException();
    }

    /**
     * Make sure the slow and fast implementation yield the same results, without triggering expiration.
     */
    @Test
    void sameNoExpiration() {

        int count = 100;

        // Make sure the expiration interval is beyond the possible timestamp advance
        long expirationInterval = (count + count/2) * TICK;

        testSame(count, expirationInterval);
    }

    /**
     * Make sure the slow and fast implementation yield the same results, triggering expiration.
     */
    @Test
    void sameWithExpiration() {

        int count = 10000;

        // Make sure the expiration interval is within the possible timestamp advance. Statistically.
        long expirationInterval = (count/10) * TICK;

        testSame(count, expirationInterval);
    }

    /**
     * Make sure the slow and fast implementation yield the same results.
     */
    private void testSame(int limit, long expirationInterval) {

        ThreadContext.push("testSame/I(" + limit + ", " + expirationInterval + ")");

        Marker m = new Marker("testSame");
        int count = 0;

        Long lastGoodTimestamp = null;

        try {

            IntegralSet dataSet2015 = new NaiveIntegralSet(expirationInterval);
            IntegralSet dataSetFast = new SlidingIntegralSet(expirationInterval);

            long timestamp = 0;

            for ( ; count < limit; count++) {

                timestamp += Math.abs(rg.nextInt(TICK)) + 1;
                double value = rg.nextDouble();

                dataSet2015.append(timestamp, value);
                dataSetFast.append(timestamp, value);

                assertThat(dataSetFast.getIntegral()).as("2015/slide").isEqualTo(dataSet2015.getIntegral(), within(0.0001));

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
    @Test
    void sameSingleExpiration80() {

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
    @Test
    void sameSingleExpiration50() {

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
    @Test
    void sameMultipleExpiration() {

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
    @Test
    void sameFirstExpiration() {

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
    private void testSameSteps(String marker, long expirationInterval, List<Long> timestamps) {

        ThreadContext.push("testSameSteps/I-" + marker);

        try {

            IntegralSet dataSet2015 = new NaiveIntegralSet(expirationInterval);
            IntegralSet dataSetFast = new SlidingIntegralSet(expirationInterval);

            long timestamp = 0;

            for (Long delta : timestamps) {

                timestamp += delta;
                double value = rg.nextDouble();

                dataSet2015.append(timestamp, value);
                dataSetFast.append(timestamp, value);

                logger.info("timestamp/expiration: {}/{}",timestamp, expirationInterval);

                double i2015 = dataSet2015.getIntegral();
                double iFast = dataSetFast.getIntegral();

                logger.debug("new/fast: {}/{}", i2015, + iFast);

                assertThat(iFast).as("2015/slide").isEqualTo(i2015, within(0.0001));
            }

            logger.debug("Success");

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

                dataSet.append(timestamp, value);

                sample(dataSet);
            }

            m.close();
            stopGate.release();
        }

        protected abstract IntegralSet createSet(long expirationInterval);
        protected abstract void sample(IntegralSet dataSet);
    }

    private class Naive extends Runner {

        Naive(long expirationInterval) throws InterruptedException {
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

    private class Sliding extends Runner {

        Sliding(long expirationInterval) throws InterruptedException {
            super(expirationInterval);
        }

        @Override
        protected IntegralSet createSet(long expirationInterval) {
            return new SlidingIntegralSet(expirationInterval);
        }

        @Override
        protected void sample(IntegralSet dataSet) {
            dataSet.getIntegral();
        }
    }
}
