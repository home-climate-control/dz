package net.sf.dz3.actuator.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.byLessThan;
import static org.assertj.core.api.Assertions.fail;

/**
 * Test cases for different delay handling strategies.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2009-2018
 */
class DelayTest {

    private final static Logger logger = LogManager.getLogger(DelayTest.class);

    /**
     * Test case to find out whether {@link ScheduledExecutorService} is suitable for
     * implementing {@code HvacController].
     */
    @Test
    void testScheduledExecutorService() throws InterruptedException {

        ThreadContext.push("testScheduledExecutorService");

        try {

            ScheduledExecutorService service = new ScheduledThreadPoolExecutor(1);

            Command c1 = new Command(50);
            Command c2 = new Command(100);
            Command c3 = new Command(150);

            long start = System.currentTimeMillis();
            long delay1 = 200;
            long delay2 = 300;
            long delay3 = 400;
            service.schedule(c1, delay1, TimeUnit.MILLISECONDS);
            service.schedule(c2, delay2, TimeUnit.MILLISECONDS);
            service.schedule(c3, delay3, TimeUnit.MILLISECONDS);

            Thread.sleep(3000);

            logger.info("c1 relative: {}", c1.getStart() - start);
            logger.info("c2 relative: {}", c2.getStart() - c1.getStart());
            logger.info("c3 relative: {}", c3.getStart() - c2.getStart());

            logger.info("c1 absolute: {}", c1.getStart() - start);
            logger.info("c2 absolute: {}", c2.getStart() - start);
            logger.info("c3 absolute: {}", c3.getStart() - start);

            {
                // VT: NOTE: Bottomline: no, this can't be used for keeping track on a command queue
                // because the delays are fixed relative to the moment when the item was scheduled, not
                // when the previous item execution was finished. It may be possible to fiddle with
                // the service implementation, but that's not what I'd like to do now.

                assertThat(c1.getStart() - start - delay1).isCloseTo(0, byLessThan(5L));
                assertThat(c2.getStart() - start - delay2).isCloseTo(0, byLessThan(5L));
                assertThat(c3.getStart() - start - delay3).isCloseTo(0, byLessThan(5L));
            }

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Test the delay for fast[er] systems.
     *
     * Any of these might fail on slow computers, or in slow (high load) environments. @Ignore them if this happens.
     */
    @Disabled("Ran almost like charm on JUnit 4, but consistently fails on JUnit 5")
    @Test
    void testDelayExact() {

        DelayedCommand c = new DelayedCommand(1000);
        var message = "Delay mismatch, or slow system (@Ignore this test if it is)";

        assertThat(c.getDelay(TimeUnit.MILLISECONDS)).isEqualTo(1000);
        assertThat(c.getDelay(TimeUnit.SECONDS)).isEqualTo(1);
        assertThat(c.getDelay(TimeUnit.MICROSECONDS)).isEqualTo(1000000);
        assertThat(c.getDelay(TimeUnit.NANOSECONDS)).isEqualTo(1000000000);
    }

    /**
     * Test the delay for slow[er] systems.
     *
     * Any of these might fail on slow computers, or in slow (high load) environments. @Ignore them if this happens.
     */
    @Test
    void testDelaySlow() {

        DelayedCommand c = new DelayedCommand(1000);

        assertDelay(1000, c.getDelay(TimeUnit.MILLISECONDS));
        assertDelay(1, c.getDelay(TimeUnit.SECONDS));
        assertDelay(1000000, c.getDelay(TimeUnit.MICROSECONDS));
        assertDelay(1000000000, c.getDelay(TimeUnit.NANOSECONDS));
    }

    private void assertDelay(long expected, long actual) {

        if (expected < actual) {
            fail("Delay mismatch (expected less than " + expected + ", actual" + actual + ")");
        }
    }

    @Test
    void testDelayQueue() throws InterruptedException {

        ThreadContext.push("testDelayQueue");

        try {

            var queue = new DelayQueue<DelayedCommand>();

            long delay1 = 50;
            long delay2 = 100;
            long delay3 = 150;
            DelayedCommand c1 = new DelayedCommand(delay1);
            DelayedCommand c2 = new DelayedCommand(delay2);
            DelayedCommand c3 = new DelayedCommand(delay3);

            queue.put(c1);
            logger.info("Queue: " + queue);

            queue.put(c2);

            logger.info("Queue: " + queue);

            queue.put(c3);
            logger.info("Queue: " + queue);

            long start = System.currentTimeMillis();

            while (!queue.isEmpty()) {

                DelayedCommand c = queue.take();

                logger.info("Running " + c);
                c.run();
            }

            logger.info("c1 relative: {}", c1.getStart() - start);
            logger.info("c2 relative: {}", c2.getStart() - c1.getStart());
            logger.info("c3 relative: {}", c3.getStart() - c2.getStart());

            {
                // VT: NOTE: Better, but still too clumsy without manipulations with
                // shared variable state.

                // The first one is really bad (why?), the rest are fine
                assertThat(c1.getStart() - start).isCloseTo(delay1, byLessThan(30L));
                assertThat(c2.getStart() - c1.getStart()).isCloseTo(delay2, byLessThan(5L));
                assertThat(c3.getStart() - c2.getStart()).isCloseTo(delay3, byLessThan(5L));
            }

        } finally {
            ThreadContext.pop();
        }
    }

    protected static class Command implements Runnable {

        public final long delayMillis;
        private long startedAt;

        public Command(long delayMillis) {
            this.delayMillis = delayMillis;
        }

        @Override
        public synchronized void run() {

            try {

                startedAt = System.currentTimeMillis();
                wait(delayMillis);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public String toString() {
            return "(delay=" + delayMillis + ", startedAt=" + startedAt + ")";
        }

        public long getStart() {
            return startedAt;
        }
    }

    protected static class DelayedCommand implements Runnable, Delayed {

        public final long delayMillis;
        private static long marker;
        private long startedAt;

        public DelayedCommand(long delayMillis) {
            this.delayMillis = delayMillis;
            marker = System.currentTimeMillis();
        }

        @Override
        public synchronized void run() {

            try {

                startedAt = System.currentTimeMillis();
                marker = startedAt;
                wait(delayMillis);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public String toString() {
            return "(delay=" + delayMillis + ", startedAt=" + startedAt + ")";
        }

        public long getStart() {
            return startedAt;
        }

        @Override
        public long getDelay(TimeUnit unit) {

            return unit.convert(marker + delayMillis - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o) {

            long diff = getDelay(TimeUnit.NANOSECONDS) - o.getDelay(TimeUnit.NANOSECONDS);
            logger.info("diff=" + diff);
            if (diff < 0) {
                return -1;
            } else if (diff > 0) {
                return 1;
            } else {
                return 0;
            }
        }
    }
}
