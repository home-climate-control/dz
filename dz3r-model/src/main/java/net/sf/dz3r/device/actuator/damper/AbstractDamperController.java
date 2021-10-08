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
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;

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

    /**
     * Last known unit status.
     */
    private Signal<UnitControlSignal, Void> unitStatus;

    private FluxSink<Pair<Signal<UnitControlSignal, Void>, Map<String, Signal<ZoneStatus, String>>>> inputSink;


    protected AbstractDamperController(Map<Zone, Damper<?>> zone2damper) {
        zone2damper.forEach((key, value) -> this.zone2damper.put(key.getAddress(), value));
    }

    @Override
    public Flux<Flux<Signal<Damper<?>, Double>>> compute(Flux<Signal<UnitControlSignal, Void>> unitFlux, Flux<Signal<ZoneStatus, String>> zoneFlux) {

        unitFlux.subscribeOn(Schedulers.boundedElastic()).subscribe(this::consumeUnit);
        zoneFlux.subscribeOn(Schedulers.boundedElastic()).subscribe(this::consumeZone);

        return Flux
                .create(this::connect)
                .map(this::compute)
                .publish()
                .autoConnect();
    }

    private void connect(FluxSink<Pair<Signal<UnitControlSignal, Void>, Map<String, Signal<ZoneStatus, String>>>> sink) {
        this.inputSink = sink;
    }

    private void consumeUnit(Signal<UnitControlSignal, Void> signal) {
        this.unitStatus = signal;
        inputSink.next(new ImmutablePair<>(unitStatus, zone2status));
    }

    private void consumeZone(Signal<ZoneStatus, String> signal) {
        zone2status.put(signal.payload, signal);
        inputSink.next(new ImmutablePair<>(unitStatus, zone2status));
    }

    private Flux<Signal<Damper<?>, Double>> compute(Pair<Signal<UnitControlSignal, Void>, Map<String, Signal<ZoneStatus, String>>> source) {

        var unitSignal = source.getKey();

        if (unitSignal.isError() || unitSignal.getValue().fanSpeed == 0) {
            return park();
        }

        return shuffle(compute(source.getValue()));
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

        return Flux.create(sink -> {
            Flux.fromIterable(positionMap.entrySet())
                    .publishOn(Schedulers.boundedElastic())
                    .doOnNext(kv -> {

                        var damper = kv.getKey();
                        var requestedPosition = kv.getValue();
                        logger.info("shuffle 1/2: {} = {}", damper, requestedPosition);

                        var done = damper.set(requestedPosition);

                        var finalPosition = done.block();
                        logger.info("shuffle 2/2: {} = {}", damper, finalPosition);

                        sink.next(new Signal<>(Instant.now(), kv.getKey(), finalPosition));

                    }).blockLast();
            sink.complete();
        });
    }

    private Flux<Signal<Damper<?>, Double>> park() {

        return Flux.create(sink -> {
            Flux.fromIterable(zone2damper.values())
                    .publishOn(Schedulers.boundedElastic())
                    .doOnNext(damper -> {

                        logger.info("park 1/2: {}", damper);

                        var done = damper.park();

                        var finalPosition = done.block();
                        logger.info("park 2/2: {} = {}", damper, finalPosition);

                        sink.next(new Signal<>(Instant.now(), damper, finalPosition));

                    }).blockLast();
            sink.complete();
        });
    }

    @Override
    public void close() throws Exception {
        park();
    }
}
