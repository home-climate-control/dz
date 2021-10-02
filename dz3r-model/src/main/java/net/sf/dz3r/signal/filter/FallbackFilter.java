package net.sf.dz3r.signal.filter;

import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.SignalProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A filter that passes the signal from the first source as long as it is not {@link Signal#isError()},
 * and then falls back to the second source, and so on until all the sources are exhausted.
 *
 * Distinct payload objects are treated as distinct sources. If a signal comes from an unrecognized source, it is discarded.
 *
 * Payload information is passed along to allow determination of which source is actually being used.
 *
 * If all sources are healthy, the output signal status will be {@link net.sf.dz3r.signal.Signal.Status#OK}.
 *
 * If some sources are not healthy, the output signal status will be {@link net.sf.dz3r.signal.Signal.Status#FAILURE_PARTIAL},
 * with the {@link Signal#error} being the error from the first faulty source in the fallback chain.
 *
 * If none of the sources are healthy, the output signal status will be {@link net.sf.dz3r.signal.Signal.Status#FAILURE_TOTAL},
 * with the {@link Signal#error} being the error from the first faulty source in the fallback chain.
 *
 * @param <T> Filtered object type.
 * @param <P> Signal source identifier.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class FallbackFilter<T, P> implements SignalProcessor<T, T, P> {

    private final Logger logger = LogManager.getLogger();

    /**
     * Ordered map of sources to their last signals.
     */
    private final Map<P, Signal<T, P>> fallbackMap = new LinkedHashMap<>();

    public FallbackFilter(List<P> fallbackChain) {
        Flux.fromIterable(fallbackChain)
                .doOnNext(s -> fallbackMap.put(s, null))
                .subscribe()
                .dispose();
    }

    @Override
    public Flux<Signal<T, P>> compute(Flux<Signal<T, P>> in) {
        return in.mapNotNull(this::fallback);
    }

    private Signal<T, P> fallback(Signal<T, P> signal) {

        P source = signal.payload;
        if (!fallbackMap.containsKey(source)) {

            // This is serious enough to complain, this stream is not supposed to contain alien signals
            logger.warn("Unrecognized source for signal {}", signal);

            // Drop it on the floor
            return null;
        }

        fallbackMap.put(source, signal);

        for (var kv : fallbackMap.entrySet()) {
            if (!kv.getValue().isError()) {

                if (kv.getKey().equals(source)) {
                    // Yes, this signal was just put there, it's the one we need to return
                    return renderSignal(kv.getValue());
                }

                // Non-error signal before the match above means that this is not the first source in the chain, drop
                return null;
            }
        }

        // Looks like all the signals are errors, let's just return the one that just came
        return signal;
    }

    private Signal<T,P> renderSignal(Signal<T,P> signal) {

        var errors = Flux.fromIterable(fallbackMap.entrySet())
                .filter(kv -> kv.getValue() != null)
                .map(Map.Entry::getValue)
                .filter(Signal::isError)
                .sort(this::reverseByTimestamp)
                .map(s -> s.error)
                .collect(Collectors.toList())
                .block();

        if (errors.isEmpty()) { // NOSONAR false positive
            // We're golden
            return signal;
        }

        // This case should've been already handled
        if (errors.size() == fallbackMap.size()) {
            throw new IllegalStateException("Should've been handled elsewhere: all " + errors.size() + " errors, map: " + fallbackMap);
        }

        return new Signal<>(signal.timestamp, signal.getValue(), signal.payload, Signal.Status.FAILURE_PARTIAL, errors.get(0));
    }

    private int reverseByTimestamp(Signal<T,P> s1, Signal<T,P> s2) {
        return s2.timestamp.compareTo(s1.timestamp);
    }
}
