package net.sf.dz3r.device.actuator;

import net.sf.dz3r.common.HCCObjects;
import net.sf.dz3r.signal.Signal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Single channel switch.
 *
 * @param <A> Address type.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2023
 */
public abstract class AbstractSwitch<A extends Comparable<A>> implements Switch<A> {

    protected final Logger logger = LogManager.getLogger();

    private final A address;
    private final Scheduler scheduler;

    /**
     * Minimum delay between two subsequent calls to {@link #setStateSync(boolean)} if the value hasn't changed.
     * See <a href="https://github.com/home-climate-control/dz/issues/253">Pipeline overrun with slow actuators</a>.
     */
    private final Duration pace;

    /**
     * Clock to use. Doesn't make sense to use a non-default clock other than for testing purposes.
     */
    private final Clock clock;

    /**
     * Assume that {@link #setState(boolean)} always worked without checking if it did. Caveat emptor.
     */
    protected final boolean optimistic;

    private final Sinks.Many<Signal<State, String>> stateSink;
    private final Flux<Signal<State, String>> stateFlux;
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
    protected AbstractSwitch(A address) {
        this(address, false, Schedulers.newSingle("switch:" + address, true), null, null);
    }

    /**
     * Create an instance with a given scheduler.
     *
     * @param address Switch address.
     * @param scheduler Scheduler to use. {@code null} means using {@link Schedulers#newSingle(String, boolean)}.
     * @param pace Issue identical control commands to this switch at most this often.
     * @param clock Clock to use. Pass {@code null} except when testing.
     */
    protected AbstractSwitch(A address, boolean optimistic, Scheduler scheduler, Duration pace, Clock clock) {

        this.address = HCCObjects.requireNonNull(address,"address can't be null");
        this.optimistic = optimistic;

        this.scheduler = scheduler;
        this.pace = pace;
        this.clock = clock == null ? Clock.systemUTC() : clock;

        stateSink = Sinks.many().multicast().onBackpressureBuffer();
        stateFlux = stateSink.asFlux();

        logger.info("{}: created AbstractSwitch({}) with optimistic={}, pace={}", Integer.toHexString(hashCode()), getAddress(), optimistic, pace);
    }

    @Override
    public final A getAddress() {
        return address;
    }

    @Override
    public final synchronized Flux<Signal<State, String>> getFlux() {
        return stateFlux;
    }

    @Override
    public final Mono<Boolean> setState(boolean state) {

        ThreadContext.push("setState");
        try {

            var cached = limitRate(state);

            return cached.map(Mono::just).orElseGet(() -> {

                reportState(new Signal<>(Instant.now(), new State(getAddress().toString(), null, state, lastKnownState)));

                try {
                    setStateSync(state);
                } catch (IOException e) {
                    return Mono.create(sink -> sink.error(e));
                }

                lastSetAt = clock.instant();

                return optimistic
                        ? Mono.just(state)
                        : getState();
            });

        } finally {
            ThreadContext.pop();
        }
    }

    private Optional<Boolean> limitRate(boolean state) {

        ThreadContext.push("limitRate");

        try {

            if (pace == null) {
                return Optional.empty();
            }

            if (lastSetState == null) {
                return Optional.empty();
            }

            if (lastSetState != state) {
                return Optional.empty();
            }

            var delay = Duration.between(lastSetAt, clock.instant());

            if (delay.compareTo(pace) < 0) {
                logger.debug("{}: skipping setState({}), too close ({} of {})", Integer.toHexString(hashCode()), state, delay, pace);
                return Optional.of(lastSetState);
            }

            return Optional.empty();

        } finally {

            lastSetState = state;
            ThreadContext.pop();
        }
    }

    private void reportState(Signal<State, String> signal) {
        stateSink.tryEmitNext(signal);
    }

    @Override
    public Mono<Boolean> getState() {
        return Mono.<Boolean>create(sink -> {
                    ThreadContext.push("getState");
                    try {

                        lastKnownState = getStateSync();
                        reportState(new Signal<>(Instant.now(), new State(getAddress().toString(), null, null, lastKnownState)));
                        sink.success(lastKnownState);

                    } catch (Throwable t) { // NOSONAR Consequences have been considered

                        reportState(new Signal<>(Instant.now(), null, null, Signal.Status.FAILURE_TOTAL, t));
                        sink.error(t);

                    } finally {
                        ThreadContext.pop();
                    }
                });

                // VT: NOTE: Having this here breaks stuff, but now that it's gone,
                // need a thorough review because likely something else is now broken.
                // More: https://github.com/home-climate-control/dz/issues/271
                // .subscribeOn(scheduler);
    }

    protected Scheduler getScheduler() {
        return scheduler;
    }

    protected abstract void setStateSync(boolean state) throws IOException;
    protected abstract boolean getStateSync() throws IOException;
}
