package net.sf.dz3.runtime.config.filter;

import net.sf.dz3.runtime.config.ConfigurationContext;
import net.sf.dz3.runtime.config.ConfigurationContextAware;
import net.sf.dz3r.signal.filter.DoubleMedianFilter;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.Optional;
import java.util.Set;

public class FilterConfigurationParser extends ConfigurationContextAware {

    public FilterConfigurationParser(ConfigurationContext context) {
        super(context);
    }

    public void parse(Set<FilterConfig> source) {

        // VT: NOTE: Some trickery might need to be required if a feed from one filter is fed as an input
        // into another; have to be smart about dependency resolution here

        for (var s: source) {
            parseMedian(s.median());
            parseMedianSet(s.medianSet());
        }
    }

    private void parseMedian(Set<MedianFilterConfig> source) {

        Flux
                .fromIterable(Optional.ofNullable(source).orElse(Set.of()))
                .subscribeOn(Schedulers.boundedElastic())
                .parallel()
                .subscribe(c -> {

                    var sensorFlux = getSensorBlocking(c.source());
                    var filter = new DoubleMedianFilter(c.depth());

                    context.sensors.register(c.id(), filter.compute(sensorFlux));
                });
    }

    private void parseMedianSet(Set<MedianSetFilterConfig> source) {
        Flux
                .fromIterable(Optional.ofNullable(source).orElse(Set.of()))
                .doOnNext(c -> logger.error("FIXME: NOT IMPLEMENTED: median set filter: id={}, sources={}", c.id(), c.sources()))
                .blockLast();
    }

}
