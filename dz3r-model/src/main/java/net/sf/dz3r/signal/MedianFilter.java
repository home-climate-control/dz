package net.sf.dz3r.signal;

import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Median filter.
 *
 * Samples from zeroth to ({@link #depth} - 1)th will produce median with their count as a filter depth.
 *
 * If some of the signals currently considered are in {@link Signal#isError()} status, the output will produce a
 * {@link Signal.Status#FAILURE_PARTIAL} status.
 *
 * If all the signals currently considered are in {@link Signal#isError()} status, the output will produce a
 * {@link Signal.Status#FAILURE_TOTAL} status.
 *
 * This signal processor does not accept any payload - it doesn't make sense in the context.
 *
 * @param <T> Filtered object type.
 *
 * @see MedianSetFilter
 * @see net.sf.dz3.device.sensor.impl.MedianFilter
 * @see net.sf.dz3.device.sensor.impl.MedianSetFilter
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public abstract class MedianFilter<T extends  Comparable<T>> implements SignalProcessor<T, T, Void> {

    /**
     * Filter depth.
     */
    public final int depth;

    private final List<Signal<T, Void>> buffer;

    protected MedianFilter(int depth) {

        // If they want to have it at 1, let them
        if (depth < 1) {
            throw new IllegalArgumentException("Unreasonable filter depth " + depth);
        }

        this.depth = depth;
        buffer = new ArrayList<>(depth);
    }

    @Override
    public Flux<Signal<T, Void>> compute(Flux<Signal<T, Void>> in) {
        return in.map(this::compute);
    }

    private Signal<T, Void> compute(Signal<T, Void> signal) {

        buffer.add(signal);

        if (buffer.size() < depth) {
            return signal;
        }

        if (buffer.size() > depth) {
            buffer.remove(0);
        }

        return filter(buffer, signal.timestamp);
    }

    /**
     * Filter the buffer.
     *
     * @param source Buffer to filter.
     * @param timestamp Timestamp to emit the signal with.
     *
     * @return The median of all signals available in the buffer.
     */
    private Signal<T, Void> filter(List<Signal<T, Void>> source, Instant timestamp) {

        // Some elements may be partial or total errors
        var errorCount = Flux.fromIterable(source).filter(Signal::isError).count().block();

        if (errorCount == source.size()) {
            // All errors, we produce an error - last one will do
            return new Signal<>(
                    timestamp,
                    null, null,
                    Signal.Status.FAILURE_TOTAL, source.get(source.size() - 1).error);
        }

        var ok = Flux.fromIterable(source).filter(Signal::isOK).count().block() == source.size();

        var result = depth % 2 == 0 ? filterEven(source) : filterOdd(source);

        // VT: FIXME: Error may not be null, but it's not that important now, can be left like this for a while
        return new Signal<>(timestamp, result, null, ok ? Signal.Status.OK : Signal.Status.FAILURE_PARTIAL, null);
    }

    private T filterEven(List<Signal<T, Void>> source) {

        return Flux.fromIterable(source)
                .map(Signal::getValue)
                .sort()
                .skip(source.size() / 2L - 1)
                .take(2)
                .reduce(this::average)
                .block();
    }

    protected abstract T average(T t1, T t2);

    private T filterOdd(List<Signal<T, Void>> source) {

        var sorted = Flux.fromIterable(source)
                .map(Signal::getValue)
                .sort()
                .collect(Collectors.toList()).block();

        return sorted.get((source.size() - 1) / 2);
    }
}
