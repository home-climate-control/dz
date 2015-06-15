package net.sf.dz3.actuator.impl;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

/**
 * Test cases for different delay handling strategies.
 *  
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2009
 */
public class DelayTest extends TestCase {

    private final static Logger logger = Logger.getLogger(DelayTest.class);
    
    /**
     * Test case to find out whether {@link ScheduledExecutorService} is suitable for
     * implementing {@link HvacController].
     * 
     * @throws InterruptedException
     */
    public void testScheduledExecutorService() throws InterruptedException {
        
        NDC.push("testScheduledExecutorService");
        
        try {
        
            ScheduledExecutorService service = new ScheduledThreadPoolExecutor(1);

            Command c1 = new Command(50);
            Command c2 = new Command(100);
            Command c3 = new Command(150);

            long start = System.currentTimeMillis();
            service.schedule(c1, 200, TimeUnit.MILLISECONDS);
            service.schedule(c2, 300, TimeUnit.MILLISECONDS);
            service.schedule(c3, 400, TimeUnit.MILLISECONDS);

            Thread.sleep(3000);

            logger.info(c1.getStart() - start);
            logger.info(c2.getStart() - c1.getStart());
            logger.info(c3.getStart() - c2.getStart());

            // VT: NOTE: Bottomline: no, this can't be used for keeping track on a command queue
            // because the delays are fixed relative to the moment when the item was scheduled, not
            // when the previous item execution was finished. It may be possible to fiddle with 
            // the service implementation, but that's not what I'd like to do now.
        
        } finally {
            NDC.pop();
        }
    }
    
    /**
     * Test the delay for fast[er] systems.
     * 
     * Any of these might fail on slow computers, or in slow (high load) environments. @Ignore them if this happens.
     */
    public void testDelayExact() {
        
        DelayedCommand c = new DelayedCommand(1000);
        String message = "Delay mismatch, or slow system (@Ignore this test if it is)";
        
        assertEquals(message, 1000, c.getDelay(TimeUnit.MILLISECONDS));
        assertEquals(message, 1, c.getDelay(TimeUnit.SECONDS));
        assertEquals(message, 1000000, c.getDelay(TimeUnit.MICROSECONDS));
        assertEquals(message, 1000000000, c.getDelay(TimeUnit.NANOSECONDS));
    }
    
    /**
     * Test the delay for slow[er] systems.
     * 
     * Any of these might fail on slow computers, or in slow (high load) environments. @Ignore them if this happens.
     */
    public void testDelaySlow() {
        
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

    public void testDelayQueue() throws InterruptedException {
        
        NDC.push("testDelayQueue");
        
        try {
        
            DelayQueue<DelayedCommand> queue = new DelayQueue<DelayedCommand>();

            DelayedCommand c1 = new DelayedCommand(50);
            DelayedCommand c2 = new DelayedCommand(100);
            DelayedCommand c3 = new DelayedCommand(150);

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

            logger.info(c1.getStart() - start);
            logger.info(c2.getStart() - c1.getStart());
            logger.info(c3.getStart() - c2.getStart());
            
            // VT: NOTE: Better, but still too clumsy without manipulations with
            // shared variable state.
        
        } finally {
            NDC.pop();
        }
    }
    
    protected static class Command implements Runnable {

        public final long delayMillis;
        private long startedAt;
        
        public Command(long delayMillis) {
            this.delayMillis = delayMillis;
        }
        
        @Override
        public void run() {
            
            try {
                
                startedAt = System.currentTimeMillis();
                Thread.sleep(delayMillis);
                
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
        public void run() {
            
            try {
                
                startedAt = System.currentTimeMillis();
                marker = startedAt;
                Thread.sleep(delayMillis);
                
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
