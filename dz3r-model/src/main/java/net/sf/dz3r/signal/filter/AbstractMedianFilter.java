package net.sf.dz3r.signal.filter;

import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.SignalProcessor;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Base class implementing common behavior features for {@link MedianFilter} and {@link MedianSetFilter}.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public abstract class AbstractMedianFilter<T extends  Comparable<T>, P> implements SignalProcessor<T, T, P> {

    /**
     * Filter depth.
     */
    public final int depth;

    protected AbstractMedianFilter(int depth) {

        // If they want to have it at 1, let them
        if (depth < 1) {
            throw new IllegalArgumentException("Unreasonable filter depth " + depth);
        }

        this.depth = depth;
    }

    @Override
    public final Flux<Signal<T, P>> compute(Flux<Signal<T, P>> in) {
        return in.map(this::compute);
    }

    protected abstract  Signal<T, P> compute(Signal<T,P> tpSignal);

    /**
     * Filter the buffer.
     *
     * @param source Buffer to filter.
     * @param timestamp Timestamp to emit the signal with.
     *
     * @return The median of all signals available in the buffer.
     */
    protected Signal<T, P> filter(List<Signal<T, P>> source, Instant timestamp) {

        var sourceSize = source.size();
        var notOkCount = new AtomicInteger();
        var errorCount = new AtomicInteger();
        List<Throwable> errors = new ArrayList<>();

        var values = Flux.fromIterable(source)
                .doOnNext(s -> {

                    if (!s.isOK()) {
                        errors.add(s.error);
                        notOkCount.incrementAndGet();
                    }

                    if (s.isError()) {
                        errorCount.incrementAndGet();
                        errors.add(s.error);
                    }
                })
                .filter(s -> s.getValue() != null)
                .collect(Collectors.toList())
                .block();


        // Some elements may be partial or total errors

        if (errorCount.intValue() == sourceSize) {
            // All errors, we produce an error - last one will do
            return new Signal<>(
                    timestamp,
                    null, null,
                    Signal.Status.FAILURE_TOTAL, errors.get(errors.size() - 1));
        }

        if (values.isEmpty()) { // NOSONAR false positive
            // Alas, not all are errors, but there are no usable values
            return new Signal<>(
                    timestamp,
                    null, null,
                    Signal.Status.FAILURE_TOTAL, errors.get(errors.size() - 1));
        }

        // Actual depth is less than requested until ramped up, or if there are errors
        var result = Math.min(values.size(), depth) % 2 == 0 ? filterEven(values) : filterOdd(values);

        return new Signal<>(timestamp, result, null,
                errors.isEmpty() ? Signal.Status.OK : Signal.Status.FAILURE_PARTIAL,
                errors.isEmpty() ? null : errors.get(errors.size() - 1));
    }

    private T filterEven(List<Signal<T, P>> source) {

        return Flux.fromIterable(source)
                .map(Signal::getValue)
                .sort()
                .skip(source.size() / 2L - 1)
                .take(2)
                .reduce(this::average)
                .block();
    }

    protected abstract T average(T t1, T t2);

    private T filterOdd(List<Signal<T, P>> source) {

        var sorted = Flux.fromIterable(source)
                .map(Signal::getValue)
                .sort()
                .collect(Collectors.toList())
                .block();

        return sorted.get((source.size() - 1) / 2); // NOSONAR false positive
    }
}
