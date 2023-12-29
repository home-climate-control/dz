package net.sf.dz3r.signal.filter;

import net.sf.dz3r.signal.Signal;

import java.util.ArrayList;
import java.util.List;

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
 * In both cases, the {@link Signal#error} reported will be the last error arrived.
 *
 * This signal processor does not accept any payload - it doesn't make sense in the context.
 *
 * @param <T> Filtered object type.
 *
 * @see MedianSetFilter
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public abstract class MedianFilter<T extends  Comparable<T>> extends AbstractMedianFilter<T, Void> {

    private final List<Signal<T, Void>> buffer;

    protected MedianFilter(int depth) {
        super(depth);

        buffer = new ArrayList<>(depth);
    }

    @Override
    protected final Signal<T, Void> compute(Signal<T, Void> signal) {

        buffer.add(signal);

        if (buffer.size() < 2) {
            // Nothing to filter yet
            return signal;
        }

        if (buffer.size() > depth) {
            buffer.remove(0);
        }

        return filter(buffer, signal.timestamp);
    }
}
