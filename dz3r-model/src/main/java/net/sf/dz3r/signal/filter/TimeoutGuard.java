package net.sf.dz3r.signal.filter;

import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.SignalProcessor;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Watch the incoming flux, and emit timeout signals either once or on every timeout interval if no input is coming.
 *
 * @param <T> Signal type.
 * @param <P> Signal payload type.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class TimeoutGuard<T, P> implements SignalProcessor<T, T, P> {

    public final Duration timeout;
    public final boolean repeat;

    private final AtomicBoolean inTimeout = new AtomicBoolean(false);

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
    }

    @Override
    public Flux<Signal<T, P>> compute(Flux<Signal<T, P>> in) {
        return in
                .windowTimeout(1, timeout)
                .publishOn(Schedulers.boundedElastic())
                .flatMap(this::timeout);
    }

    private Flux<Signal<T, P>> timeout(Flux<Signal<T,P>> source) {

        var window = source
                .collectList()
                .block();

        if (window.isEmpty()) { // NOSONAR false positive

            try {

                if (repeat || !inTimeout.get()) {

                    return Flux.just(new Signal<>(
                            Instant.now(),
                            null,
                            null,
                            Signal.Status.FAILURE_TOTAL,
                            new TimeoutException(String.format("Timeout of %s is exceeded", timeout.toString()))));
                } else {
                    return Flux.empty();
                }

            } finally {
                inTimeout.set(true);
            }
        }

        inTimeout.set(false);
        return Flux.just(window.get(0));
    }
}
