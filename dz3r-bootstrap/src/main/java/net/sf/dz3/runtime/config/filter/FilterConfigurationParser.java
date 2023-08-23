package net.sf.dz3.runtime.config.filter;

import net.sf.dz3.runtime.config.ConfigurationContext;
import net.sf.dz3.runtime.config.ConfigurationContextAware;
import net.sf.dz3r.signal.filter.DoubleMedianFilter;
import net.sf.dz3r.signal.filter.DoubleMedianSetFilter;
import reactor.core.publisher.Flux;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class FilterConfigurationParser extends ConfigurationContextAware {

    public FilterConfigurationParser(ConfigurationContext context) {
        super(context);
    }

    public void parse(Set<FilterConfig> source) {

        // VT: NOTE: Some trickery might need to be required if a feed from one filter is fed as an input
        // into another; have to be smart about dependency resolution here

        for (var s: Optional.ofNullable(source).orElse(Set.of())) {
            parseMedian(s.median());
            parseMedianSet(s.medianSet());
        }
    }

    private void parseMedian(Set<MedianFilterConfig> source) {

        Flux
                .fromIterable(Optional.ofNullable(source).orElse(Set.of()))
                .subscribe(c -> {

                    var sensorFlux = getSensorBlocking(c.source());
                    var filter = new DoubleMedianFilter(c.depth());

                    context.sensors.register(c.id(), filter.compute(sensorFlux));
                });
    }

    private void parseMedianSet(Set<MedianSetFilterConfig> source) {

        Flux
                .fromIterable(Optional.ofNullable(source).orElse(Set.of()))
                .subscribe(c -> {

                    // VT: FIXME: This construct will get stuck and get mum if a requested sensor flux is not available

                    var sources = Flux
                            .fromIterable(c.sources())
                            .map(this::getSensorBlocking)
                            .collect(Collectors.toSet())
                            .block();

                    var filter = new DoubleMedianSetFilter<String>(sources.size());

                    context.sensors.register(c.id(), filter.compute(sources));
                });
    }
}
