package net.sf.dz3r.signal.filter;

import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.SignalProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeoutException;

/**
 * Watch the incoming flux, and emit timeout signals either once or on every timeout interval if no input is coming.
 *
 * @param <T> Signal type.
 * @param <P> Signal payload type.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class TimeoutGuard<T, P> implements SignalProcessor<T, T, P>, AutoCloseable {

    private final Logger logger = LogManager.getLogger();

    public final Duration timeout;
    public final boolean repeat;

    private boolean inTimeout = false;
    private Instant lastSeenAt;

    private final Flux<Signal<T,P>> timeoutFlux;
    private final Disposable timeoutFluxSubscription;
    private FluxSink<Signal<T,P>> timeoutFluxSink;

    private final Thread guardThread;

    /**
     * Create a non-repeating instance.
     *
     * This is the preferred way of dealing with timeouts, generating extra traffic is counterproductive
     * and may mask design problems.
     *
     * @param timeout Timeout to observe.
     */
    public TimeoutGuard(Duration timeout) {
        this(timeout, false);
    }

    /**
     * Create an instance.
     *
     * @param timeout Timeout to observe.
     * @param repeat If {@code true}, keep emitting a timeout error signal every {@link #timeout}, otherwise just once.
     */
    public TimeoutGuard(Duration timeout, boolean repeat) {

        this.timeout = timeout;
        this.repeat = repeat;

        lastSeenAt = Instant.now();

        timeoutFlux = Flux.create(this::connect);
        timeoutFluxSubscription = timeoutFlux.subscribe();

        guardThread = new Thread(this::guard);
        guardThread.start();
    }

    private void connect(FluxSink<Signal<T, P>> sink) {
        this.timeoutFluxSink = sink;
    }

    private synchronized void guard() {
        ThreadContext.push("guard");
        try {

            while (!Thread.interrupted()) {

                var now = Instant.now();
                var leftToWait = timeout.minus(Duration.between(lastSeenAt, now));

                logger.debug("Left to wait: {}ms", leftToWait.toMillis());

                if ((leftToWait.toMillis() <= 0) && (!inTimeout || repeat)) {
                    generateTimeoutSignal(now);
                    continue;
                }

                wait(leftToWait.toMillis());
            }

            logger.warn("Interrupted, terminating");

        } catch (Throwable t) { // NOSONAR This is intended
            logger.fatal("Unexpected exception, guard thread is gone", t);
        } finally {
            ThreadContext.clearAll();
        }
    }

    private void generateTimeoutSignal(Instant now) {

        logger.warn("generating timeout signal for {}", now);

        timeoutFluxSink.next(new Signal<>(
                        now,
                        null,
                        null,
                        Signal.Status.FAILURE_TOTAL,
                        new TimeoutException(String.format("Timeout of %s is exceeded", timeout))));

        inTimeout = true;
        lastSeenAt = now;
    }

    @Override
    public Flux<Signal<T, P>> compute(Flux<Signal<T, P>> in) {

        var actual = in
                .doOnNext(s -> touch(s.timestamp))
                .doOnNext(ignored -> inTimeout = false)
                .doOnNext(s -> logger.warn("  signal: {}", s));

        var guard = timeoutFlux
                .doOnNext(s -> logger.warn("  guard:  {}", s));

        return Flux
                .merge(actual, guard)
                .log();
    }

    private synchronized void touch(Instant timestamp) {
        lastSeenAt = timestamp;
        logger.warn("set lastSeenAt={}", lastSeenAt);
        notifyAll();
    }

    @Override
    public void close() {
        timeoutFluxSubscription.dispose();
        guardThread.interrupt();
    }
}
