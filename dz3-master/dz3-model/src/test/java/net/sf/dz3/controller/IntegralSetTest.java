package net.sf.dz3.controller;

import junit.framework.TestCase;
import net.sf.dz3.controller.pid.IntegralSet;
import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.Semaphore;

/**
 * @author <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2012
 */
public class IntegralSetTest extends TestCase {

    private final Logger logger = Logger.getLogger(getClass());

    private final Random rg = new SecureRandom();
    private Semaphore startGate = new Semaphore(2);
    private Semaphore stopGate = new Semaphore(2);

    private static final long INTEGRATION_INTERVAL = 10000L;
    private static final int COUNT = 10000;

    /**
     * Compare slow and fast implementation speed.
     */
    public void testAll() throws InterruptedException {

        startGate.acquire(2);

        Thread t1 = new Thread(new Slow());
        Thread t2 = new Thread(new Fast());

        t1.start();
        t2.start();

        startGate.release(2);
        logger.info("unleashed");

        stopGate.acquire(2);

        logger.info("done");
    }

    /**
     * Make sure the slow and fast implementation yield the same results.
     */
    @SuppressWarnings("deprecation")
    public void testSame() {

        NDC.push("testSame/I");

        try {

            IntegralSet dataSet = new IntegralSet(INTEGRATION_INTERVAL);

            long start = System.currentTimeMillis();
            long timestamp = start;

            for (int count = 0; count < COUNT; count++) {

                timestamp += rg.nextInt(100);
                double value = rg.nextDouble();

                dataSet.record(timestamp, value);

                assertEquals(dataSet.getIntegralSlow(), dataSet.getIntegral());
            }

            long now = System.currentTimeMillis();

            logger.info((now - start) + "ms");

        } finally {
            NDC.pop();
        }

    }

    private abstract class Runner implements Runnable {

        Runner() throws InterruptedException {
            stopGate.acquire();
        }

        public void run() {

            IntegralSet dataSet = new IntegralSet(INTEGRATION_INTERVAL);

            try {
                startGate.acquire();
            } catch (InterruptedException e) {
                logger.info("Interrupted", e);
            }

            long start = System.currentTimeMillis();
            long now = start;

            for (int count = 0; count < COUNT; count++) {

                now += rg.nextInt(100);
                double value = rg.nextDouble();

                dataSet.record(now, value);

                sample(dataSet);
            }

            long stop = System.currentTimeMillis();

            logger.info(getClass().getName() + " Completed in " + (stop - start));
            stopGate.release();
        }

        protected abstract void sample(IntegralSet dataSet);
    }

    private class Fast extends Runner {

        Fast() throws InterruptedException {
            super();
        }

        @Override
        protected void sample(IntegralSet dataSet) {
            dataSet.getIntegral();
        }
    }

    private class Slow extends Runner {

        Slow() throws InterruptedException {
            super();
        }

        @SuppressWarnings("deprecation")
        @Override
        protected void sample(IntegralSet dataSet) {
            dataSet.getIntegralSlow();
        }
    }
}
