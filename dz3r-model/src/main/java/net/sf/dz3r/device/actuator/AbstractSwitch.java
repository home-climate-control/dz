package net.sf.dz3r.device.actuator;

import net.sf.dz3r.signal.Signal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
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

    private Flux<Signal<State, String>> stateFlux;
    private FluxSink<Signal<State, String>> stateSink;
    private Boolean lastKnownState;

    /**
     * Create an instance with a default scheduler.
     *
     * @param address Switch address.
     */
    protected AbstractSwitch(A address) {
        this(address, null);
    }

    /**
     * Create an instance with a default scheduler.
     *
     * @param address Switch address.
     * @param scheduler Scheduler to use.
     */
    protected AbstractSwitch(A address, Scheduler scheduler) {

        if (address == null) {
            throw new IllegalArgumentException("address can't be null");
        }

        this.address = address;
        this.scheduler = scheduler == null ? Schedulers.newSingle("switch:" + address, true) : scheduler;
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

        logger.debug("Creating state flux for {}", address);

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

        return Mono.<Boolean>create(sink -> {
                    ThreadContext.push("setState");
                    try {

                        reportState(new Signal<>(Instant.now(), new State(state, lastKnownState)));
                        setStateSync(state);

                        lastKnownState = getStateSync();
                        reportState(new Signal<>(Instant.now(), new State(state, lastKnownState)));
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
    public final Mono<Boolean> getState() {
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
