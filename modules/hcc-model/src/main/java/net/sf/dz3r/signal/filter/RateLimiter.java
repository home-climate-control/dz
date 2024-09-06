package net.sf.dz3r.signal.filter;

import com.homeclimatecontrol.hcc.signal.Signal;
import net.sf.dz3r.signal.SignalProcessor;
import reactor.core.publisher.Flux;

import java.time.Duration;

/**
 * Watch the incoming flux, and drop incoming signals if they are effectively identical for the consumer,
 * but only if they came earlier than a given duration after the last signal.
 *
 * @param <T> Signal type.
 * @param <P> Signal payload type.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 * @see TimeoutGuard
 */
public class RateLimiter<T, P> implements SignalProcessor<T, T, P> {

    @FunctionalInterface
    public interface Comparator<T, P> {
        boolean equals(Signal<T, P> o1, Signal<T, P> o2);
    }

    private final Duration delay;
    private final Comparator<T, P> comparator;

    private Signal<T, P> lastSignal;

    /**
     * Create an instance.
     *
     * @param delay Minimum gap between signals to keep.
     * @param comparator Comparator to use to compare incoming signals. If {@code null}, then {@link Equals} is used.
     */
    public RateLimiter(Duration delay, Comparator<T, P> comparator) {

        this.delay = delay;
        this.comparator = comparator == null ? new Equals<>() : comparator;
    }

    @Override
    public Flux<Signal<T, P>> compute(Flux<Signal<T, P>> in) {

        return in.flatMap(s -> {

            if (lastSignal == null) {

                // First time, go on
                lastSignal = s;
                return Flux.just(s);
            }

            if (!comparator.equals(lastSignal, s)) {

                // No contest, they are different
                return Flux.just(s);
            }

            if (Duration.between(lastSignal.timestamp(), s.timestamp()).compareTo(delay) < 0) {

                // Too early, keep quiet
                return Flux.empty();
            }

            // All right, twist my arm
            lastSignal = s;
            return Flux.just(s);
        });
    }

    /**
     * Default comparator. This is most likely NOT what you want.
     */
    public static class Equals<T, P> implements Comparator<T, P> {

        @Override
        public boolean equals(Signal<T, P> o1, Signal<T, P> o2) {
            return o1 == o2;
        }
    }
}
