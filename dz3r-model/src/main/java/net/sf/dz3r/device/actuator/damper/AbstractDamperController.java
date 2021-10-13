package net.sf.dz3r.device.actuator.damper;

import net.sf.dz3r.model.Zone;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.hvac.UnitControlSignal;
import net.sf.dz3r.signal.hvac.ZoneStatus;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base damper controller logic.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public abstract class AbstractDamperController implements DamperController {

    protected final Logger logger = LogManager.getLogger();

    /**
     * Initial set, with zone names instead of names.
     */
    private final Map<String, Damper<?>> zone2damper = new TreeMap<>();

    /**
     * Last known zone status. Error signals also matter.
     */
    private final Map<String, Signal<ZoneStatus, String>> zone2status = new TreeMap<>();

    private final Flux<Flux<Signal<Damper<?>, Double>>> outputFlux;

    /**
     * Last known unit status.
     */
    private Signal<UnitControlSignal, Void> unitStatus;

    private Scheduler controlScheduler = Schedulers.newSingle("damper controller", true);
    private FluxSink<Pair<Signal<UnitControlSignal, Void>, Map<String, Signal<ZoneStatus, String>>>> controlSink;


    protected AbstractDamperController(Map<Zone, Damper<?>> zone2damper) {

        if (zone2damper.isEmpty()) {
            throw new IllegalStateException("No zone to damper mapping provided, need at least two pairs");
        }

        if (zone2damper.size() == 1) {
            throw new IllegalStateException("Damper controller with just one damper doesn't make sense. Just skip it altogether.");
        }

        zone2damper.forEach((key, value) -> this.zone2damper.put(key.getAddress(), value));

        outputFlux = Flux
                .create(this::connect)
                .map(this::compute)
                .publish()
                .autoConnect();

        new Thread(() -> {
            outputFlux
                    .doOnNext(positionSet -> {
                        logger.debug("positionSet: {}", positionSet);
                        var counter = new AtomicInteger();
                        positionSet
                                .doOnNext(e -> logger.debug("position: {}", e))
                                .doOnNext(ignored -> counter.incrementAndGet())
                                .blockLast();
                        logger.debug("positionSet: {} items", counter.get());
                    })
                    .doOnComplete(() -> logger.debug("control thread: complete"))
                    .blockLast();
        }).start();
    }

    @Override
    public Flux<Flux<Signal<Damper<?>, Double>>> compute(Flux<Signal<UnitControlSignal, Void>> unitFlux, Flux<Signal<ZoneStatus, String>> zoneFlux) {

        unitFlux.publishOn(Schedulers.boundedElastic()).subscribe(this::consumeUnit);
        zoneFlux.publishOn(Schedulers.boundedElastic()).subscribe(this::consumeZone);

        return outputFlux;
    }

    private void connect(FluxSink<Pair<Signal<UnitControlSignal, Void>, Map<String, Signal<ZoneStatus, String>>>> sink) {
        this.controlSink = sink;
    }

    private void consumeUnit(Signal<UnitControlSignal, Void> signal) {
        logger.debug("consumeUnit: {}", signal);
        this.unitStatus = signal;
        controlSink.next(new ImmutablePair<>(unitStatus, getZone2status(null)));
    }


    private void consumeZone(Signal<ZoneStatus, String> signal) {
        logger.debug("consumeZone: {}", signal);
        controlSink.next(new ImmutablePair<>(unitStatus, getZone2status(signal)));
    }

    /**
     * Get the zone to status mapping.
     *
     * Unless this is done in a controlled way, {@code ConcurrentModificationException} on a collision of the iterator with
     * the addition is inevitable.
     */
    private synchronized Map<String, Signal<ZoneStatus, String>>  getZone2status(Signal<ZoneStatus, String> signal) {

        if (signal != null) {
            zone2status.put(signal.payload, signal);
        }

        return new TreeMap<>(zone2status);
    }

    private Flux<Signal<Damper<?>, Double>> compute(Pair<Signal<UnitControlSignal, Void>, Map<String, Signal<ZoneStatus, String>>> source) {

        var unitSignal = source.getKey();
        logger.debug("compute");

        if (unitSignal.isError() || unitSignal.getValue().fanSpeed == 0) {
            return park();
        }

        return shuffle(compute(source.getValue()));
    }

    protected final Damper<?> getDamperFor(String zoneName) {
        return zone2damper.get(zoneName);
    }

    protected abstract Map<Damper<?>, Double> compute(Map<String, Signal<ZoneStatus, String>> source);

    /**
     * Shuffle the dampers.
     *
     * @param positionMap The map of the damper to the position it needs to be put in.
     *
     * @return A concrete flux of damper positions after the execution of this operation. External subscription
     * is not required to move the dampers.
     */
    private Flux<Signal<Damper<?>, Double>> shuffle(Map<Damper<?>, Double> positionMap) {
        logger.debug("shuffle");

        return Flux.create(sink -> {
            Flux.fromIterable(positionMap.entrySet())
                    .publishOn(Schedulers.boundedElastic())
                    .doOnNext(kv -> {

                        var damper = kv.getKey();
                        var requestedPosition = kv.getValue();
                        logger.debug("shuffle 1/2: {} = {}", damper, requestedPosition);

                        var done = damper.set(requestedPosition);

                        var finalPosition = done.block();
                        logger.debug("shuffle 2/2: {} = {}", damper, finalPosition);

                        sink.next(new Signal<>(Instant.now(), kv.getKey(), finalPosition));

                    }).blockLast();
            sink.complete();
        });
    }

    private Flux<Signal<Damper<?>, Double>> park() {
        logger.debug("park");

        return Flux.create(sink -> {
            Flux.fromIterable(zone2damper.values())
                    .publishOn(Schedulers.boundedElastic())
                    .doOnNext(damper -> {

                        logger.debug("park 1/2: {}", damper);

                        var done = damper.park();

                        var finalPosition = done.block();
                        logger.debug("park 2/2: {} = {}", damper, finalPosition);

                        sink.next(new Signal<>(Instant.now(), damper, finalPosition));

                    }).blockLast();
            sink.complete();
        });
    }

    @Override
    public void close() throws Exception {
        logger.warn("close()...");
        park();
        controlSink.complete();
        controlScheduler.dispose();
        logger.info("closed.");
    }
}
