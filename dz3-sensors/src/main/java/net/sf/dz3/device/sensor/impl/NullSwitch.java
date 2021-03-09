package net.sf.dz3.device.sensor.impl;

import java.io.IOException;
import java.util.Random;

import org.apache.logging.log4j.ThreadContext;

import net.sf.jukebox.jmx.JmxDescriptor;

/**
 * Null switch.
 *
 * Does absolutely nothing other than reflecting itself in the log and via JMX.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com"> Vadim Tkachenko 2001-2020
 */
public class NullSwitch extends AbstractSwitch {

    private static final Random rg = new Random();

    private final long minDelay;
    private final int maxDelay;
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
     * @param minDelay Minimim switch deley, milliseconds.
     * @param maxDelay Max delay. Total delay is calculated as {@code minDelay + rg.nextInt(maxDelay)}.
     */
    public NullSwitch(String address, long minDelay, int maxDelay, Object semaphore) {
        super(address, false);

        this.minDelay = minDelay;
        this.maxDelay = maxDelay;
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

    private void delay() {

        if (minDelay == 0 && maxDelay == 0) {
            return;
        }

        try {

            long delay = minDelay + rg.nextInt(maxDelay);

            if (semaphore != null) {

                // Simulate the lock on a common resource
                synchronized (semaphore) {
                    Thread.sleep(delay);
                }

            } else {

                // No lock, just sleep
                Thread.sleep(delay);
            }

            logger.info("slept {}ms", delay);

        } catch (InterruptedException ex) {
            // Oh well,no delay.
        }
    }
}
