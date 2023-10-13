package net.sf.dz3r.runtime.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;

import java.util.Optional;
import java.util.Set;

/**
 * Resolves switches and sensors from configuration elements.
 *
 * @param <T> Configuration element type.
 */
public abstract class SensorSwitchResolver<T> {

    protected final Logger logger = LogManager.getLogger();

    protected final Set<T> source;

    protected SensorSwitchResolver(Set<T> source) {
        this.source = Optional.ofNullable(source).orElse(Set.of());
    }

    public abstract Flux<Id2Flux> getSensorFluxes();
}
