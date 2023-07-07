package net.sf.dz3.runtime.config.filter;

import net.sf.dz3.runtime.config.ConfigurationContext;
import net.sf.dz3.runtime.config.ConfigurationContextAware;
import reactor.core.publisher.Flux;

import java.util.Optional;
import java.util.Set;

public class FilterConfigurationParser extends ConfigurationContextAware {

    public FilterConfigurationParser(ConfigurationContext context) {
        super(context);
    }

    public void parse(Set<FilterConfig> source) {

        // VT: FIXME: In order to create these filters, sources have to be already available, and even though
        // they are instantiated, they have not been made available yet.

        // VT: NOTE: Some trickery might need to be required if a feed from one filter is fed as an input
        // into another; have to be smart about dependency resolution here

        for (var s: source) {
            parseMedian(s.median());
            parseMedianSet(s.medianSet());
        }

        logger.error("FIXME: signal filters are not created yet");
    }

    private void parseMedian(Set<MedianFilterConfig> source) {

        Flux
                .fromIterable(Optional.ofNullable(source).orElse(Set.of()))
                .doOnNext(c -> logger.warn("FIXME: median filter: id={}, depth={}, source={}", c.id(), c.depth(), c.source()))
                .blockLast();
    }
    private void parseMedianSet(Set<MedianSetFilterConfig> source) {
        Flux
                .fromIterable(Optional.ofNullable(source).orElse(Set.of()))
                .doOnNext(c -> logger.warn("FIXME: median set filter: id={}, sources={}", c.id(), c.sources()))
                .blockLast();
    }

}
