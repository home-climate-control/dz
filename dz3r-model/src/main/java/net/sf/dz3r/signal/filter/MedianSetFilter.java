package net.sf.dz3r.signal.filter;

import net.sf.dz3r.signal.Signal;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Median set filter.
 *
 * The filter will yield the median of values available from all incoming samples,
 * with distinct payload objects being treated as distinct sources.
 *
 * Signals in {@link Signal#isError()} status will disqualify their sources from participation in median calculation.
 *
 * If some sources produce error signals, the output signal will produce a {@link Signal.Status#FAILURE_PARTIAL} status.
 *
 * If all sources produce error signals, the output signal will produce a {@link Signal.Status#FAILURE_TOTAL} status.
 *
 * Payload information is discarded along the way.
 *
 * @param <T> Filtered object type.
 * @param <P> Signal source identifier.
 *
 * @see MedianFilter
 * @see net.sf.dz3.device.sensor.impl.MedianFilter
 * @see net.sf.dz3.device.sensor.impl.MedianSetFilter
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public abstract class MedianSetFilter<T extends  Comparable<T>, P> extends AbstractMedianFilter<T, P> {

    private final Map<P, Signal<T, P>> channelMap = new HashMap<>();

    /**
     * Create an instance with a given depth.
     *
     * @param depth A misnomer for this kind of filter - this better be equal to the number of signal sources.
     */
    protected MedianSetFilter(int depth) {
        super(depth);
    }

    @Override
    protected final Signal<T, P> compute(Signal<T, P> signal) {

        channelMap.put(signal.payload, signal);

        var buffer = new ArrayList<Signal<T, P>>(channelMap.size());
        Flux.fromIterable(channelMap.values())
                .sort(this::sortByTimestamp)
                .subscribe(buffer::add);

        if (buffer.size() < 2) { // NOSONAR false positive
            // Nothing to filter yet
            return signal;
        }

        if (buffer.size() > depth) {
            buffer.remove(0);
        }

        return filter(buffer, signal.timestamp);
    }

    private int sortByTimestamp(Signal<T, P> s1, Signal<T, P> s2) {
        return s1.timestamp.compareTo(s2.timestamp);
    }
}
