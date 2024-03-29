package net.sf.dz3r.device.actuator;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Optional;
import java.util.Random;

/**
 * Does absolutely nothing other than fulfilling the API contract and reflecting itself in the log (and later via JMX).
 *
 * A useful tool for troubleshooting a configuration without actual hardware available.
 *
 * @deprecated Use {@link NullCqrsSwitch} instead.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2021
 */
@Deprecated(since = "5.0.0")
public class NullSwitch extends AbstractSwitch<String> {

    private static final Random rg = new SecureRandom();

    private final long minDelayMillis;
    private final int maxDelayMillis;

    private Boolean state;

    /**
     * Create a pessimistic instance without delay running on the default scheduler.
     *
     * @param address Address to use.
     */
    public NullSwitch(String address) {
        this(address, false, 0, 0, Schedulers.newSingle("NullSwitch:" + address, true));
    }

    /**
     * Create an instance without delay running on the default scheduler.
     *
     * @param address Address to use.
     * @param optimistic See <a href="https://github.com/home-climate-control/dz/issues/280">issue 280</a>.
     */
    public NullSwitch(String address, boolean optimistic) {
        this(address, optimistic, 0, 0, Schedulers.newSingle("NullSwitch:" + address, true));
    }

    /**
     * Create a pessimistic instance without delay running on the provided scheduler.
     *
     * @param address Address to use.
     * @param scheduler Scheduler to use.
     */
    public NullSwitch(String address, Scheduler scheduler) {
        this(address, false, 0, 0, scheduler);
    }

    /**
     * Create an instance without delay running on the provided scheduler.
     *
     * @param address Address to use.
     * @param scheduler Scheduler to use.
     */
    public NullSwitch(String address, boolean optimistic, Scheduler scheduler) {
        this(address, optimistic, 0, 0, scheduler);
    }

    /**
     * Create an instance with delay.
     *
     * @param address Address to use.
     * @param minDelayMillis Minimim switch deley, milliseconds.
     * @param maxDelayMillis Max delay. Total delay is calculated as {@code minDelay + rg.nextInt(maxDelay)}.
     */
    public NullSwitch(String address, boolean optimistic, long minDelayMillis, int maxDelayMillis, Scheduler scheduler) {
        super(address, optimistic, scheduler, null, null);

        if (minDelayMillis < 0 || maxDelayMillis < 0 || (maxDelayMillis > 0 && (minDelayMillis >= maxDelayMillis))) {
            throw new IllegalArgumentException("invalid delays min=" + minDelayMillis + ", max=" + maxDelayMillis);
        }

        this.minDelayMillis = minDelayMillis;
        this.maxDelayMillis = maxDelayMillis;
    }

    @Override
    protected void setStateSync(boolean state) throws IOException {
        long delayMillis = getDelayMillis();
        logger.debug("setState({})={} delay={}ms", getAddress(), state, delayMillis);

        delay(delayMillis);

        this.state = state;
    }

    /**
     * Get the state.
     *
     * @return {@link #state}.
     * @throws IOException if {@link #setStateSync(boolean)} was not called yet and {@link #state} is {@code null}.
     */
    @Override
    protected boolean getStateSync() throws IOException {
        long delayMillis = getDelayMillis();
        logger.debug("getState({})={} delay={}ms", getAddress(), state, delayMillis);

        delay(delayMillis);

        return Optional.ofNullable(state).orElseThrow(() -> new IOException("setStateSync() hasn't been called yet on " + getAddress()));
    }

    private long getDelayMillis() {
        if (minDelayMillis == 0 && maxDelayMillis == 0) {
            return 0;
        }

        return minDelayMillis + rg.nextInt(maxDelayMillis);
    }

    private void delay(long delayMillis) {

        if (delayMillis > 0) {
            try {
                Mono.delay(Duration.ofMillis(delayMillis), getDelayScheduler()).block();
            } catch (IllegalStateException ex) {
                if (ex.getMessage().contains("block()/blockFirst()/blockLast() are blocking, which is not supported in thread")) {
                    logger.warn("{}: delay() on non-blocking thread (name={}, group={}), using Thread.sleep() workaround",
                            getAddress(),
                            Thread.currentThread().getName(),
                            Thread.currentThread().getThreadGroup().getName());
                    try {
                        Thread.sleep(delayMillis);
                    } catch (InterruptedException ex2) {
                        Thread.currentThread().interrupt();
                        logger.warn("Interrupted, ignored", ex2);
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        return "NullSwitch(" + getAddress() + ")";
    }

    /**
     * @return A scheduler used only for {@link Mono#delayElement(Duration)}.
     */
    private Scheduler getDelayScheduler() {

        // Parallel scheduler is the default for delayElement() anyway
        return getScheduler() == null
                ? Schedulers.parallel()
                : getScheduler();
    }

    @Override
    public Mono<Boolean> getState() {

        if (true) { // NOSONAR Shut up. I know.
            return super.getState();
        }

        // VT: NOTE: While this is the Reactive compliant solution, currently it breaks at least the HeatPump.
        // More: https://github.com/home-climate-control/dz/issues/279

        if (state == null) {
            return Mono.error(new IOException("setStateSync() hasn't been called yet on " + getAddress()));
        }

        return Mono.just(state).delayElement(Duration.ofMillis(getDelayMillis()), getDelayScheduler());
    }
}
