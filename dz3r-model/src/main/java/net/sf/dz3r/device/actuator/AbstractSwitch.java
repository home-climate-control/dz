package net.sf.dz3r.device.actuator;

import net.sf.dz3r.common.HCCObjects;
import net.sf.dz3r.signal.Signal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.annotation.NonNull;
import reactor.util.annotation.Nullable;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Single channel switch.
 *
 * @param <A> Address type.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2021
 */
public abstract class AbstractSwitch<A extends Comparable<A>> implements Switch<A> {

    protected final Logger logger = LogManager.getLogger();

    private final A address;
    private final Scheduler scheduler;

    /**
     * Minimum delay between two subsequent calls to {@link #setStateSync(boolean)} if the value hasn't changed.
     * See <a href="https://github.com/home-climate-control/dz/issues/253">Pipeline overrun with slow actuators</a>.
     */
    private final Duration minDelay;

    /**
     * Clock to use. Doesn't make sense to use a non-default clock other than for testing purposes.
     */
    private final Clock clock;

    private Flux<Signal<State, String>> stateFlux;
    private FluxSink<Signal<State, String>> stateSink;
    private Boolean lastKnownState;

    /**
     * Ugly hack to counter the manipulations of {@link #lastKnownState} by some subclasses. That needs to be redone.
     */
    private Boolean lastSetState;
    private Instant lastSetAt;

    /**
     * Create an instance with a default scheduler.
     *
     * @param address Switch address.
     */
    protected AbstractSwitch(@NonNull A address) {
        this(address, null, null, null);
    }

    /**
     * Create an instance with a given scheduler.
     *
     * @param address Switch address.
     * @param scheduler Scheduler to use. {@code null} means using {@link Schedulers#newSingle(String, boolean)}.
     * @param minDelay Minimum delay between sending identical commands to hardware.
     * @param clock Clock to use. Pass {@code null} except when testing.
     */
    protected AbstractSwitch(@NonNull A address, @Nullable Scheduler scheduler, @Nullable Duration minDelay, @Nullable Clock clock) {

        // VT: NOTE: @NonNull seems to have no effect, what enforces it?
        this.address = HCCObjects.requireNonNull(address,"address can't be null");

        this.scheduler = scheduler == null ? Schedulers.newSingle("switch:" + address, true) : scheduler;
        this.minDelay = minDelay;
        this.clock = clock == null ? Clock.systemUTC() : clock;

        logger.info("{}: created AbstractSwitch({}) with minDelay={}", Integer.toHexString(hashCode()), getAddress(), minDelay);
    }

    @Override
    public final A getAddress() {
        return address;
    }

    @Override
    public final synchronized Flux<Signal<State, String>> getFlux() {

        if (stateFlux != null) {
            return stateFlux;
        }

        logger.debug("{}: creating stateFlux:{}", Integer.toHexString(hashCode()), address);

        stateFlux = Flux
                .create(this::connect)
                .doOnSubscribe(s -> logger.debug("stateFlux:{} subscribed", address))
                .publishOn(Schedulers.boundedElastic())
                .publish()
                .autoConnect();

        return stateFlux;
    }

    private void connect(FluxSink<Signal<State, String>> sink) {
        this.stateSink = sink;
    }

    @Override
    public final Mono<Boolean> setState(boolean state) {

        ThreadContext.push("setState");
        try {

            var cached = limitRate(state);

            if (cached != null) {
                return Mono.just(cached);
            }

            reportState(new Signal<>(Instant.now(), new State(state, lastKnownState)));
            setStateSync(state);

            lastSetAt = clock.instant();

            return getState();

        } catch (IOException e) {
            return Mono.create(sink -> sink.error(e));
        } finally {
            ThreadContext.pop();
        }
    }

    private Boolean limitRate(boolean state) {

        ThreadContext.push("limitRate");

        try {

            if (minDelay == null) {
                return null;
            }

            if (lastSetState == null) {
                return null;
            }

            if (lastSetState != state) {
                return null;
            }


            var delay = Duration.between(lastSetAt, clock.instant());

            if (delay.compareTo(minDelay) < 0) {
                logger.debug("{}: skipping setState({}), too close ({} of {})", Integer.toHexString(hashCode()), state, delay, minDelay);
                return lastSetState;
            }

            return null;

        } finally {

            lastSetState = state;
            ThreadContext.pop();
        }
    }

    private void reportState(Signal<State, String> signal) {

        if (stateSink == null) {

            // Unless something subscribes, this will be flooding the log - enable for troubleshooting
            // logger.warn("stateSink:{} is still null, skipping: {}", address, signal); // NOSONAR

            getFlux();
            return;
        }

        stateSink.next(signal);
    }

    @Override
    public Mono<Boolean> getState() {
        return Mono.<Boolean>create(sink -> {
                    ThreadContext.push("getState");
                    try {

                        lastKnownState = getStateSync();
                        reportState(new Signal<>(Instant.now(), new State(null, lastKnownState)));
                        sink.success(lastKnownState);

                    } catch (Throwable t) { // NOSONAR Consequences have been considered

                        reportState(new Signal<>(Instant.now(), null, null, Signal.Status.FAILURE_TOTAL, t));
                        sink.error(t);

                    } finally {
                        ThreadContext.pop();
                    }
                })
                .subscribeOn(scheduler);
    }

    protected Scheduler getScheduler() {
        return scheduler;
    }

    protected abstract void setStateSync(boolean state) throws IOException;
    protected abstract boolean getStateSync() throws IOException;
}
