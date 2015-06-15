package net.sf.dz3.controller;

import junit.framework.TestCase;
import net.sf.dz3.controller.pid.DifferentialSet;

import java.util.Random;
import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

/**
 * @author <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2012
 */
public class DifferentialSetTest extends TestCase {

    private final Logger logger = Logger.getLogger(getClass());

    private final Random rg = new Random();
    private Semaphore startGate = new Semaphore(2);
    private Semaphore stopGate = new Semaphore(2);

    private final long INTEGRATION_INTERVAL = 10000L;
    private final int COUNT = 10000;

    /**
     * Compare slow and fast implementation speed.
     */
    public void testAll() throws InterruptedException {

        startGate.acquire(2);

        Thread t1 = new Thread(new DifferentialSetTest.Slow());
        Thread t2 = new Thread(new DifferentialSetTest.Fast());

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

        NDC.push("testSame/D");

        try {

            DifferentialSet dataSet = new DifferentialSet(INTEGRATION_INTERVAL);

            long start = System.currentTimeMillis();
            long timestamp = start;

            for (int count = 0; count < COUNT; count++) {

                timestamp += rg.nextInt(100);
                double value = rg.nextDouble();

                dataSet.record(timestamp, value);

                assertEquals(dataSet.getDifferentialSlow(), dataSet.getDifferential());
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

            DifferentialSet dataSet = new DifferentialSet(INTEGRATION_INTERVAL);

            try {
                startGate.acquire();
            } catch (InterruptedException e) {
                logger.warn("Interrupted", e);
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

        protected abstract void sample(DifferentialSet dataSet);
    }

    private class Fast extends DifferentialSetTest.Runner {

        Fast() throws InterruptedException {
            super();
        }

        @Override
        protected void sample(DifferentialSet dataSet) {
            dataSet.getDifferential();
        }
    }

    private class Slow extends DifferentialSetTest.Runner {

        Slow() throws InterruptedException {
            super();
        }

        @SuppressWarnings("deprecation")
        @Override
        protected void sample(DifferentialSet dataSet) {
            dataSet.getDifferentialSlow();
        }
    }
}
