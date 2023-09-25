package net.sf.dz3r.device.actuator.damper;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Random;

/**
 * Does absolutely nothing other than fulfilling the API contract and reflecting itself in the log (and later via JMX).
 *
 * A useful tool for troubleshooting a configuration without actual hardware available.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2021
 */
public class NullDamper extends AbstractDamper<String> {

    private static final Random rg = new SecureRandom();

    private final long minDelayMillis;
    private final int maxDelayMillis;

    /**
     * Current position.
     */
    private double position = DEFAULT_PARK_POSITION;

    /**
     * Create an instance without delay running on the default scheduler.
     *
     * @param address Address to use.
     */
    public NullDamper(String address) {
        this(address, 0, 0, Schedulers.newSingle("NullDamper", true));
    }

    /**
     * Create an instance with delay.
     *
     * @param address Address to use.
     * @param minDelayMillis Minimim switch deley, milliseconds.
     * @param maxDelayMillis Max delay. Total delay is calculated as {@code minDelay + rg.nextInt(maxDelay)}.
     */
    public NullDamper(String address, long minDelayMillis, int maxDelayMillis, Scheduler scheduler) {
        super(address, scheduler);

        if (minDelayMillis < 0 || maxDelayMillis < 0 || (maxDelayMillis > 0 && (minDelayMillis >= maxDelayMillis))) {
            throw new IllegalArgumentException("invalid delays min=" + minDelayMillis + ", max=" + maxDelayMillis);
        }

        this.minDelayMillis = minDelayMillis;
        this.maxDelayMillis = maxDelayMillis;
    }

    @Override
    public Mono<Double> set(double position) {

        checkPosition(position);

        this.position = position;
        long delayMillis = getDelayMillis();
        try {
            logger.info("set({})={} {} delay={}ms", getAddress(), position, delayMillis > 0 ? "1/2" : "1/1", delayMillis);
            return Mono.just(position).delayElement(Duration.ofMillis(delayMillis));
        } finally {
            if (delayMillis > 0) {
                logger.info("set({})={} 2/2 delay={}ms", getAddress(), position, delayMillis);
            }
        }
    }

    /**
     * Get the position.
     *
     * Use this only for troubleshooting and test cases; this method is not included into the {@link Damper} API.
     *
     * @return Current position.
     */
    public Double get() {
        return position;
    }

    private long getDelayMillis() {
        if (minDelayMillis == 0 && maxDelayMillis == 0) {
            return 0;
        }

        return minDelayMillis + rg.nextInt(maxDelayMillis);
    }

    @Override
    public String toString() {
        return "{NullDamper: address=" + getAddress() + ", position=" + position + ", park=" + getParkPosition() + "}";
    }
}
