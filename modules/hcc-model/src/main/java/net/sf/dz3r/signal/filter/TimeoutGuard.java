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
public class TimeoutGuard<T, P> implements SignalProcessor<T, T, P> {

    private final Logger logger = LogManager.getLogger();

    public final String marker;

    public final Duration timeout;
    public final boolean repeat;

    private boolean inTimeout = false;
    private Instant lastSeenAt;

    private final Flux<Signal<T,P>> timeoutFlux;
    private final Disposable timeoutFluxSubscription;
    private FluxSink<Signal<T,P>> timeoutFluxSink;

    private final Thread guardThread;

    /**
     * Create an instance.
     *
     * @param marker A unique marker to trace the control flow in the logs.
     * @param timeout Timeout to observe.
     * @param repeat If {@code true}, keep emitting a timeout error signal every {@link #timeout}, otherwise just once.
     */
    public TimeoutGuard(String marker, Duration timeout, boolean repeat) {

        if (timeout.minus(Duration.ofMillis(1)).isNegative()) {
            throw new IllegalArgumentException("Unreasonably short timeout of " + timeout);
        }

        this.marker = marker;
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

                logger.trace("{}: leftToWait={}, inTimeout={}, repeat={}", marker, leftToWait, inTimeout, repeat);

                if ((leftToWait.toMillis() <= 0) && (!inTimeout || repeat)) {
                    generateTimeoutSignal(now);
                }

                if (leftToWait.toMillis() <= 0) {
                    touch(now);
                    continue;
                }

                wait(leftToWait.toMillis());
            }

            logger.warn("{}: interrupted, terminating", marker);

        } catch (Throwable t) { // NOSONAR This is intended
            logger.fatal("{}: unexpected exception, guard thread is gone", marker, t);
        } finally {
            ThreadContext.clearAll();
        }
    }

    private void generateTimeoutSignal(Instant now) {

        logger.info("{}: timeout of {} is exceeded, inTimeout={}, repeat={}", marker, timeout, inTimeout, repeat);

        timeoutFluxSink.next(new Signal<>(
                        now,
                        null,
                        null,
                        Signal.Status.FAILURE_TOTAL,
                        new TimeoutException(String.format("%s: timeout of %s is exceeded", marker, timeout))));

        inTimeout = true;
    }

    @Override
    public Flux<Signal<T, P>> compute(Flux<Signal<T, P>> in) {

        var actual = in
                .doOnNext(s -> touch(s.timestamp))
                .doOnNext(ignored -> inTimeout = false)
                .doOnError(t -> logger.error("{}: errored out", marker, t))
                .doOnComplete(this::close);

        return Flux.merge(actual, timeoutFlux)
                .doOnNext(s -> logger.trace("{}: compute={}", marker, s))
                .doOnError(t -> logger.error("{}: errored out", marker, t))
                .doOnComplete(() -> logger.debug("{}: completed", marker));
    }

    private synchronized void touch(Instant timestamp) {
        lastSeenAt = timestamp;
        notifyAll();
    }

    private void close() {
        timeoutFluxSink.complete();
        timeoutFluxSubscription.dispose();
        guardThread.interrupt();
    }
}
