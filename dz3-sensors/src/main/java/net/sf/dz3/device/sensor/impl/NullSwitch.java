package net.sf.dz3.device.sensor.impl;

import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;
import org.apache.logging.log4j.ThreadContext;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Random;

/**
 * Null switch.
 *
 * Does absolutely nothing other than reflecting itself in the log and via JMX.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2020
 */
public class NullSwitch extends AbstractSwitch {

    private static final Random rg = new SecureRandom();

    private final long minDelayMillis;
    private final int maxDelayMillis;
    private final Object semaphore;

    /**
     * Create an instance without delay.
     *
     * @param address Address to use.
     */
    public NullSwitch(String address) {
        this(address, 0, 0, null);
    }

    /**
     * Create an instance with delay.
     *
     * @param address Address to use.
     * @param minDelayMillis Minimim switch deley, milliseconds.
     * @param maxDelayMillis Max delay. Total delay is calculated as {@code minDelay + rg.nextInt(maxDelay)}.
     */
    public NullSwitch(String address, long minDelayMillis, int maxDelayMillis, Object semaphore) {
        super(address, false);

        this.minDelayMillis = minDelayMillis;
        this.maxDelayMillis = maxDelayMillis;
        this.semaphore = semaphore;
    }

    @Override
    public JmxDescriptor getJmxDescriptor() {

        return new JmxDescriptor(
                "dz",
                "Null Switch",
                Integer.toHexString(hashCode()),
                "Pretends to be the actual switch");
    }

    @Override
    public synchronized boolean getState() throws IOException {
        delay();
        return super.getState();
    }

    @Override
    public synchronized void setState(boolean state) throws IOException {
        ThreadContext.push("setState/delay");
        try {
            delay();
            super.setState(state);
        } finally {
            ThreadContext.pop();
        }
    }

    @SuppressWarnings({"squid::S2273","squid::S2274"})
    private void delay() {

        if (minDelayMillis == 0 && maxDelayMillis == 0) {
            return;
        }

        try {

            long delay = minDelayMillis + rg.nextInt(maxDelayMillis);

            if (semaphore != null) {

                // Simulate the lock on a common resource
                synchronized (semaphore) {
                    // squid::S2274: Works as designed.
                    wait(delay);
                }

            } else {

                // No lock, just sleep
                // squid::S2274: Works as designed.
                wait(delay);
            }

            logger.info("slept {}ms", delay);

        } catch (InterruptedException ex) {
            // Oh well,no delay.
            Thread.currentThread().interrupt();
        }
    }
}
