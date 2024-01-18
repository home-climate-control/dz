package net.sf.dz3r.counter;

import net.sf.dz3r.common.HCCObjects;
import reactor.core.publisher.Flux;

/**
 * Base class for just counting. Doesn't concern itself with persistence.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public abstract class AbstractUsageCounter<T extends Comparable<T>> implements ResourceUsageCounter<T> {

    private T current;
    private final T threshold;

    protected AbstractUsageCounter(T current, T threshold) {

        HCCObjects.requireNonNull(current, "current can't be null");

        this.current = current;
        this.threshold = threshold;
    }

    @Override
    public final Flux<State<T>> consume(Flux<T> increments) {

        return increments
                .map(this::increment)
                .map(this::report);
    }

    private State<T> report(T current) {
        return new State<>(current, threshold);
    }

    private synchronized T increment(T delta) {
        current = add(current, delta);
        return current;
    }

    protected abstract T add(T current, T increment);
}
