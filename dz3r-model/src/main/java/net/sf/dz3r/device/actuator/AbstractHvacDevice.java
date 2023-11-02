package net.sf.dz3r.device.actuator;

import net.sf.dz3r.counter.DurationIncrementAdapter;
import net.sf.dz3r.counter.ResourceUsageCounter;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.hvac.HvacDeviceStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Common functionality for all HVAC device drivers.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public abstract class AbstractHvacDevice<T> implements HvacDevice<T> {

    protected final Logger logger = LogManager.getLogger();
    protected final Clock clock;

    private final String name;

    private final Sinks.Many<Signal<HvacDeviceStatus<T>, Void>> statusSink;
    private final Flux<Signal<HvacDeviceStatus<T>, Void>> statusFlux;

    /**
     * The moment this device turned on, {@code null} if currently off.
     */
    private Instant startedAt;

    private boolean isClosed;

    private final Disposable uptimeCounterSubscription;

    protected AbstractHvacDevice(String name) {
        this(Clock.systemUTC(), name, null);
    }

    protected AbstractHvacDevice(
            String name,
            ResourceUsageCounter<Duration> uptimeCounter
    ) {
        this(Clock.systemUTC(), name, uptimeCounter);
    }

    protected AbstractHvacDevice(
            Clock clock,
            String name,
            ResourceUsageCounter<Duration> uptimeCounter
    ) {
        this.clock = clock;
        this.name = name;

        statusSink = Sinks.many().multicast().onBackpressureBuffer();
        statusFlux = statusSink.asFlux();


        if (uptimeCounter != null) {

            var uptimeFlux = getFlux()
                    .flatMap(this::getUptime);
            var converter = new DurationIncrementAdapter();
            uptimeCounterSubscription = uptimeCounter
                    .consume(converter.split(uptimeFlux))
                    .subscribe();
        } else {
            uptimeCounterSubscription = null;
        }
    }

    @Override
    public final String getAddress() {
        return name;
    }

    protected void check(CqrsSwitch<?> s, String purpose) {
        if (s == null) {
            throw new IllegalArgumentException("'" + purpose + "' switch can't be null");
        }
    }

    @Override
    public final Flux<Signal<HvacDeviceStatus<T>, Void>> getFlux() {
        return statusFlux;
    }

    protected final void broadcast(Signal<HvacDeviceStatus<T>, Void> signal) {
        logger.debug("{}: broadcast: {}", getAddress(), signal);
        statusSink.tryEmitNext(signal);
    }

    /**
     * Update uptime.
     *
     * @param timestamp Timestamp to associate the uptime start with.
     * @param state {@code true} if on.
     */
    protected final synchronized void updateUptime(Instant timestamp, boolean state) {

        if (state) {

            if (startedAt == null) {
                startedAt = timestamp;
            }

        } else {

            startedAt = null;
        }
    }

    /**
     * Get the device uptime.
     *
     * @return For how long the device has been turned on, {@code null} if currently off.
     */
    public Duration uptime() {
        return startedAt == null ? null : Duration.between(startedAt, Instant.now());
    }

    private Flux<Duration> getUptime(Signal<HvacDeviceStatus<T>, Void> signal) {

        if (signal.isError()) {
            return Flux.empty();
        }

        // Null uptime will be in the signal when the HVAC is off
        return Flux.just(Objects.requireNonNullElse(signal.getValue().uptime, Duration.ZERO));
    }

    protected boolean isClosed() {
        return isClosed;
    }

    @Override
    public final void close() throws IOException {

        if (isClosed) {
            logger.warn("redundant close(), ignored");
            return;
        }

        isClosed = true;
        Optional
                .ofNullable(uptimeCounterSubscription)
                .ifPresent(Disposable::dispose);
        doClose();
    }

    protected abstract void doClose() throws IOException;
}
