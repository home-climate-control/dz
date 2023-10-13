package net.sf.dz3r.counter;

import reactor.core.publisher.Flux;

/**
 * Usage model converter.
 *
 * {@link ResourceUsageCounter#consume(Flux)} consumes increments. Old model used monotonously increasing snapshots
 * interspersed with zeros to do the same thing. The old model can be considered a superset of new and doesn't need
 * to be reimplemented for scratch, this adapter is what is doing its job.
 *
 * @param <T> Measured data type.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public abstract class AbsoluteIncrementAdapter<T extends Comparable<T>> {

    /**
     * Last known snapshot
     */
    private T lastKnown;

    /**
     * Consume a flux of snapshots, return a flux of increments.
     */
    public final Flux<T> split(Flux<T> snapshots) {
        return snapshots.flatMap(this::convert);
    }

    private synchronized Flux<T> convert(T snapshot) {

        if (lastKnown == null) {

            // This is the first snapshot in series, unless it is a terminator

            if (isZero(snapshot)) {
                return Flux.empty();
            }

            lastKnown = snapshot;
            return Flux.just(snapshot);
        }

        var temp = lastKnown;

        if (isZero(snapshot)) {

            // Received a sequence terminator
            lastKnown = null;
            return Flux.empty();
        }

        if (!isMonotonous(lastKnown, snapshot)) {
            return Flux.error(new IllegalArgumentException("lastKnown=" + lastKnown + ", snapshot=" + snapshot + ", not monotonous"));
        }

        lastKnown = snapshot;

        return Flux.just(diff(temp, snapshot));
    }

    /**
     * Determine if the given snapshot is a terminator of a monotonous sequence.
     */
    protected abstract boolean isZero(T snapshot);

    /**
     * @return {@code true} if {@code lastKnown} < {@code snapshot}.
     */
    protected abstract boolean isMonotonous(T lastKnown, T snapshot);

    /**
     * Get the difference between the old and new values.
     */
    protected abstract T diff(T older, T newer);
}
