package net.sf.dz3r.signal.filter;

import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.SignalProcessor;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeoutException;

/**
 * Watch the incoming flux, and emit timeout signals every timeout interval if no input is coming.
 *
 * @param <T> Signal type.
 * @param <P> Signal payload type.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class TimeoutGuard<T, P> implements SignalProcessor<T, T, P> {

    public final Duration timeout;

    public TimeoutGuard(Duration timeout) {
        this.timeout = timeout;
    }

    @Override
    public Flux<Signal<T, P>> compute(Flux<Signal<T, P>> in) {
        return in
                .windowTimeout(1, timeout)
                .publishOn(Schedulers.boundedElastic())
                .map(this::timeout);
    }

    private Signal<T, P> timeout(Flux<Signal<T,P>> source) {

        var window = source
                .collectList()
                .block();

        if (window.isEmpty()) {
            return new Signal<>(
                    Instant.now(),
                    null,
                    null,
                    Signal.Status.FAILURE_TOTAL,
                    new TimeoutException(String.format("Timeout of %s is exceeded", timeout.toString())));
        }

        return window.get(0);
    }
}
