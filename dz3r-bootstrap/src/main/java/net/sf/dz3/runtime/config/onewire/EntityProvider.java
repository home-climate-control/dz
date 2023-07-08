package net.sf.dz3.runtime.config.onewire;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

import java.util.LinkedHashMap;
import java.util.Map;

public class EntityProvider<T> implements AutoCloseable {

    protected final Logger logger = LogManager.getLogger();
    private final String kind;
    private final Map<String, T> address2entity = new LinkedHashMap<>();
    private final Flux<Map.Entry<String, T>> flux;
    private FluxSink<Map.Entry<String, T>> sink;
    private final Disposable tracker;

    public EntityProvider(String kind) {

        this.kind = kind;

        flux = Flux
                .create(this::connect)
                .cache();
        tracker = flux
                .subscribeOn(Schedulers.newSingle("tracker:" + kind, false))
                .subscribe();
    }

    private void connect(FluxSink<Map.Entry<String, T>> sink) {
        this.sink = sink;
    }

    /**
     * Register the entity and emit it via {@link #getFlux()}.
     */
    public void register(String key, T entity) {

        logger.info("{} available: {}", kind, key);

        address2entity.put(key, entity);
        sink.next(new ImmutablePair<>(key, entity));
    }

    /**
     * Mark the flux complete. Call this when it is clear that there will be no more entities of this type coming.
     */
    @Override
    public void close() {

        sink.complete();
        tracker.dispose();

        logger.debug("closed: {}", kind);
    }

    public synchronized Flux<Map.Entry<String, T>> getFlux() {
        return flux;
    }
}
