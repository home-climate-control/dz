package net.sf.dz3r.runtime.config.onewire;

import net.sf.dz3r.device.Addressable;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.LinkedHashMap;
import java.util.Map;

public class EntityProvider<T> implements AutoCloseable {

    protected final Logger logger = LogManager.getLogger();
    private final String kind;
    private final Map<String, T> address2entity = new LinkedHashMap<>();
    private final Sinks.Many<Map.Entry<String, T>> sink;
    private final Flux<Map.Entry<String, T>> flux;

    public EntityProvider(String kind) {

        this.kind = kind;

        sink = Sinks.many().replay().all();
        flux = sink.asFlux();
    }

    /**
     * Register the entity and emit it via {@link #getFlux()}.
     */
    public void register(String key, T entity) {

        ThreadContext.push(kind + "#" + Integer.toHexString(hashCode()));
        try {
            logger.info("{} available: {}", kind, key);

            address2entity.put(key, entity);
            sink.tryEmitNext(new ImmutablePair<>(key, entity));
        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Mark the flux complete. Call this when it is clear that there will be no more entities of this type coming.
     */
    @Override
    public void close() {

        sink.tryEmitComplete();

        ThreadContext.push(kind + "#" + Integer.toHexString(hashCode()));
        try {

            logger.debug("{} collected", address2entity.size());

            for (var address : address2entity.keySet()) {
                logger.debug("  {}", address);
            }

            logger.debug("closed: {}", kind);
        } finally {
            ThreadContext.pop();
        }
    }

    public Flux<Map.Entry<String, T>> getFlux() {
        return flux;
    }

    public T getById(String consumer, String id) {

        var found = getFlux()
                .filter(kv -> kv.getKey().equals(id))
                .blockFirst();

        if (found != null) {
            return found.getValue();
        }

        logger.error("{}: \"{}\" not found among configured {} IDs; existing mappings follow:", consumer, id, kind);
        getFlux()
                .subscribe(entry -> logger.error(
                        "  id={}, {}={}",
                        entry.getKey(),
                        kind,
                        entry.getValue() instanceof Addressable<?> a
                                ? a.getAddress()
                                : entry.getValue()));

        logger.error("{}: {}: skipping to proceed with the rest of the configuration", consumer, id);

        return null;
    }

    public Mono<T> getMonoById(String consumer, String id) {
        var found = getById(consumer, id);

        return found == null
                ? Mono.empty()
                : Mono.just(found);
    }
}
