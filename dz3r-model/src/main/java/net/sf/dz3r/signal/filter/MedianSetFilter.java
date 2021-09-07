package net.sf.dz3r.signal.filter;

import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.SignalProcessor;
import reactor.core.publisher.Flux;

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
 * @param <T> Filtered object type.
 * @param <P> Signal payload type.
 *
 * @see MedianFilter
 * @see net.sf.dz3.device.sensor.impl.MedianFilter
 * @see net.sf.dz3.device.sensor.impl.MedianSetFilter
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class MedianSetFilter<T extends  Comparable<T>, P> implements SignalProcessor<T, T, P> {

    @Override
    public Flux<Signal<T, P>> compute(Flux<Signal<T, P>> in) {
        throw new UnsupportedOperationException("Not Implemented");
    }
}
