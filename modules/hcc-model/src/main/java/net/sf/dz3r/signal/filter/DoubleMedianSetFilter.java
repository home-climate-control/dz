package net.sf.dz3r.signal.filter;

import net.sf.dz3r.signal.Signal;
import reactor.core.publisher.Flux;

import java.util.HashSet;
import java.util.Set;

public class DoubleMedianSetFilter<P> extends MedianSetFilter<Double, P> {

    public DoubleMedianSetFilter(int depth) {
        super(depth);
    }

    @Override
    protected Double average(Double d1, Double d2) {
        return (d1 + d2) / 2;
    }

    public Flux<Signal<Double, Void>> compute(Set<Flux<Signal<Double, Void>>> sourceSet) {

        var filter = new DoubleMedianSetFilter<Integer>(sourceSet.size());
        var filterSet = new HashSet<Flux<Signal<Double, Integer>>>();

        for (var flux : sourceSet) {
            var hash = flux.hashCode();
            filterSet.add(flux
                    // Convert the (Double, Void) signal to (Double, Integer)
                    // so that the filter can distinguish channels
                    .map(s -> new Signal<>(s.timestamp, s.getValue(), hash, s.status, s.error)));
        }

        return filter
                .compute(Flux.merge(filterSet))
                // Strip the hash, the consumer doesn't need it
                .map(s -> new Signal<>(s.timestamp, s.getValue(), null, s.status, s.error));
    }
}
