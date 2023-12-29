package net.sf.dz3r.counter;

import reactor.core.publisher.Flux;

/**
 * Resource usage counter.
 *
 * @param <T> Measured data type.
 *
 * @see ResourceUsageReporter
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public interface ResourceUsageCounter<T extends Comparable<T>> {

    /**
     * Consume a stream of readings, and report the current usage.
     *
     * @param increments Flux of increments to be added to {@link State#current}.
     */
    Flux<State<T>> consume(Flux<T> increments);

    /**
     * Counter state.
     *
     * @param current Current consumption.
     * @param threshold Consumption threshold (100% is when {@link #current} equals {@link #threshold}). {@code null} means none.
     * @param <T> Data type.
     */
    record State<T extends Comparable<T>>(
            T current,
            T threshold
    ) {

    }
}
