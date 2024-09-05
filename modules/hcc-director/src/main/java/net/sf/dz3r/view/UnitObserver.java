package net.sf.dz3r.view;

import com.homeclimatecontrol.hcc.signal.Signal;
import com.homeclimatecontrol.hcc.signal.hvac.HvacDeviceStatus;
import net.sf.dz3r.model.UnitDirector;
import net.sf.dz3r.signal.hvac.ZoneStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.AbstractMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Keeps track on the state of everything connected to a given {@link UnitDirector}.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2024
 */
public class UnitObserver {

    private final Logger logger = LogManager.getLogger();

    private final UnitDirector source;

    private final Map<String, Signal<Double, Void>> zone2sensor = new TreeMap<>();
    private final Map<String, Signal<ZoneStatus, String>> zone2status = new TreeMap<>();
    private Signal<HvacDeviceStatus, Void> unitStatus;

    /**
     * Feed terminators.
     *
     * VT: FIXME: Do we really need them? They have never been acted upon on shutdown. Consider removing.
     */
    private final Set<Disposable> terminators = new LinkedHashSet<>();

    public UnitObserver(UnitDirector source) {
        this.source = source;

        init();
    }

    private void init() {
        ThreadContext.push("init");

        try {

            logger.info("Observer feeds: initializing...");

            var feed = source.getFeed();

            Flux.fromIterable(feed.sensorFlux2zone.entrySet())
                    .map(kv -> new AbstractMap.SimpleEntry<>(
                            kv.getValue().getAddress(),
                            kv.getKey()))
                    .map(kv -> {
                        return kv.getValue()
                                .doOnNext(kv2 -> zone2sensor.put(kv.getKey(), kv2))
                                .publishOn(Schedulers.boundedElastic())
                                .subscribe();
                    })
                    .doOnNext(terminators::add)
                    .subscribe().dispose();

            terminators.add(feed.aggregateZoneFlux
                    .doOnNext(s -> {
                        logger.trace("Zone status: {}: {}", s.payload, s);
                        zone2status.put(s.payload, s);
                    })
                    .publishOn(Schedulers.boundedElastic())
                    .subscribe());

            terminators.add(
                    feed.hvacDeviceFlux
                            .doOnNext(s -> {
                                logger.trace("Unit status: {}", s);
                                this.unitStatus = s;
                            })
                            .publishOn(Schedulers.boundedElastic())
                            .subscribe());

            logger.info("Observer feeds: {} initialized", terminators.size());

        } finally {

            ThreadContext.pop();
        }
    }

    public Flux<Map.Entry<String, Signal<ZoneStatus, String>>> getZones() {
        return Flux.fromIterable(zone2status.entrySet());
    }

    public Mono<Signal<ZoneStatus, String>> getZone(String name) {
        // Inefficient, but simpler to implement. Tolerable on this set size.
        return getZones()
                .filter(kv -> kv.getKey().equals(name))
                .map(Map.Entry::getValue)
                .next();
    }

    public Flux<Map.Entry<String, Signal<Double, Void>>> getSensors() {
        return Flux.fromIterable(zone2sensor.entrySet());
    }

    public Mono<Signal<Double, Void>> getSensor(String name) {
        // Inefficient, but simpler to implement. Tolerable on this set size.
        return getSensors()
                .filter(kv -> kv.getKey().equals(name))
                .map(Map.Entry::getValue)
                .next();
    }

    public Signal<HvacDeviceStatus, Void> getUnitStatus() {
        return unitStatus;
    }
}
