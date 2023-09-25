package net.sf.dz3r.model;

import net.sf.dz3r.device.Addressable;
import net.sf.dz3r.device.actuator.HvacDevice;
import net.sf.dz3r.scheduler.ScheduleUpdater;
import net.sf.dz3r.scheduler.Scheduler;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.hvac.HvacCommand;
import net.sf.dz3r.signal.hvac.HvacDeviceStatus;
import net.sf.dz3r.signal.hvac.UnitControlSignal;
import net.sf.dz3r.signal.hvac.ZoneStatus;
import net.sf.dz3r.view.Connector;
import net.sf.dz3r.view.MetricsCollector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * Assembles all the components related to one hardware HVAC unit, connects them, and manages their lifecycles.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class UnitDirector implements Addressable<String>, AutoCloseable {

    private final Logger logger = LogManager.getLogger();

    private final String name;

    private final Feed feed;

    private final CountDownLatch sigTerm = new CountDownLatch(1);
    private final CountDownLatch shutdownComplete = new CountDownLatch(1);

    /**
     * Create an instance.
     *
     * @param scheduleUpdater Schedule updater to listen to event feed from.
     * @param metricsCollectorSet Set of sinks to send all the metrics to.
     * @param connectorSet Set of connectors to use.
     * @param name Instance name.
     * @param sensorFlux2zone Mapping of sensor flux to zone it will feed.
     * @param unitController Unit controller for this set of zones.
     * @param hvacDevice HVAC device that serves this set of zones.
     * @param hvacMode HVAC device mode for this zone. {@link Thermostat} PID signals must have correct polarity for this mode.
     */
    public UnitDirector(
            String name,
            ScheduleUpdater scheduleUpdater,
            Set<MetricsCollector> metricsCollectorSet,
            Set<Connector> connectorSet,
            Map<Flux<Signal<Double, Void>>, Zone> sensorFlux2zone,
            UnitController unitController,
            HvacDevice hvacDevice,
            HvacMode hvacMode
    ) {

        this.name = name;

        var scheduleFlux = Optional.ofNullable(scheduleUpdater)
                .map(u -> connectScheduler(sensorFlux2zone.values(), u))
                .orElseGet(() -> {
                    logger.warn("{}: no scheduler provided, running defaults", getAddress());
                    return Flux.empty();
                });

        // This is necessary because... <facepalm> the architecture is screwed up and applying the schedule to the zone
        // is a side effect of consuming this flux. No wonder the schedule was only applied when the console was up,
        // it was the only consumer until now.
        // See https://github.com/home-climate-control/dz/issues/281

        scheduleFlux
                .publishOn(Schedulers.newSingle("schedule-watcher-" + name))
                .doOnNext(s -> logger.debug("{}: zone={}, event={}", name, s.getKey(), s.getValue()))
                .subscribe();

        feed = connectFeeds(sensorFlux2zone, unitController, hvacDevice, hvacMode, scheduleFlux);

        var zones = sensorFlux2zone.values();

        Optional.ofNullable(metricsCollectorSet)
                .ifPresent(collectors -> Flux.fromIterable(collectors)
                        .publishOn(Schedulers.boundedElastic())
                        .doOnNext(c -> c.connect(feed))
                        .doOnComplete(() -> logger.info("{}: connected metric collectors", getAddress()))
                        .subscribe());

        Optional.ofNullable(connectorSet)
                .ifPresent(connectors -> Flux.fromIterable(connectors)
                        .publishOn(Schedulers.boundedElastic())
                        .doOnNext(c -> {
                            c.connect(feed);
                            // VT: FIXME: Connect the control input when the API signature is established
                        })
                        .doOnComplete(() -> logger.info("{}: connected connectors", getAddress()))
                        .subscribe());

        logger.info("Configured: {} ({} zones: {})",
                name,
                zones.size(),
                Flux.fromIterable(zones)
                        .map(Zone::getAddress)
                        .sort()
                        .collectList()
                        .block());

        Flux.just(Instant.now())
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(this::start)
                .subscribe();
    }

    private Feed connectFeeds(
            Map<Flux<Signal<Double, Void>>, Zone> sensorFlux2zone,
            UnitController unitController,
            HvacDevice hvacDevice,
            HvacMode hvacMode,
            Flux<Map.Entry<String, Map.Entry<SchedulePeriod, ZoneSettings>>> scheduleFlux) {

        var aggregateZoneFlux = Flux
                .merge(extractSensorFluxes(sensorFlux2zone))
                .publish().autoConnect()
                .checkpoint("aggregate-sensor");
        var zoneControllerFlux = new ZoneController(sensorFlux2zone.values())
                .compute(aggregateZoneFlux)
                .publish().autoConnect()
                .checkpoint("zone-controller")
                .map(this::stripZoneName);
        var unitControllerFlux = unitController
                .compute(zoneControllerFlux)
                .publish().autoConnect()
                .checkpoint("unit-controller");
        var hvacDeviceFlux = hvacDevice
                .compute(
                        Flux.concat(
                                Flux.just(new Signal<>(Instant.now(), new HvacCommand(hvacMode, null, null))),
                                unitControllerFlux
                        )).publish().autoConnect()
                .checkpoint("hvac-device");

        return new Feed(
                getAddress(),
                sensorFlux2zone,
                aggregateZoneFlux,
                zoneControllerFlux,
                unitControllerFlux,
                hvacDeviceFlux,
                scheduleFlux
        );
    }

    private Flux<Map.Entry<String, Map.Entry<SchedulePeriod, ZoneSettings>>> connectScheduler(Collection<Zone> zones, ScheduleUpdater scheduleUpdater) {

        net.sf.dz3r.common.HCCObjects.requireNonNull(scheduleUpdater, "programming error, this should've been resolved up the call stack");

        var name2zone = Flux.fromIterable(zones)
                .map(z -> new AbstractMap.SimpleEntry<>(z.getAddress(), z))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                .block();

        logger.info("{}: connected schedules: {}", getAddress(), name2zone);

        var scheduler = new Scheduler(name2zone);

        return scheduler.connect(scheduleUpdater.update());
    }

    @Override
    public String getAddress() {
        return name;
    }

    private Set<Flux<Signal<ZoneStatus, String>>> extractSensorFluxes(Map<Flux<Signal<Double, Void>>, Zone> sensorFlux2zone) {

        return Flux.fromIterable(sensorFlux2zone.entrySet())
                .map(this::addZoneName)
                .map(kv -> kv.getValue().compute(kv.getKey()))
                .collect(Collectors.toSet()).block();
    }

    /**
     * Convert a {@code Pair<Flux<Signal<Double, Void>>, Zone>} into a {@code Pair<Flux<Signal<Double, String>>, Zone>}, with the string being the zone name.
     *
     * @param sensorFlux2zone Mapping from the sensor flux to the zone it feeds.
     *
     * @return Source mapping with {@code Void} replaced by {@code zone.getAddress()}.
     */
    private Map.Entry<Flux<Signal<Double, String>>, Zone> addZoneName(Map.Entry<Flux<Signal<Double, Void>>, Zone> sensorFlux2zone) {

        var sensorFlux = sensorFlux2zone.getKey();
        var zone = sensorFlux2zone.getValue();
        var zoneName = zone.getAddress();

        return new AbstractMap.SimpleEntry<>(
                sensorFlux
                        .map(s -> new Signal<>(s.timestamp, s.getValue(), zoneName, s.status, s.error)), zone);
    }

    private Signal<UnitControlSignal, Void> stripZoneName(Signal<UnitControlSignal, String> s) {
        return new Signal<>(s.timestamp, s.getValue(), null, s.status, s.error);
    }

    public final Flux<Signal<HvacDeviceStatus, Void>> getFlux() {
        return feed.hvacDeviceFlux;
    }

    private void start(Instant startedAt) {

        ThreadContext.push("run");
        try {

            logger.info("Starting the pipeline: {}", getAddress());
            var theEnd = feed.hvacDeviceFlux
                    .publishOn(Schedulers.boundedElastic())
                    .subscribe(
                            s -> {
                                logger.debug("HVAC device: {}", s);
                            },
                            error -> {
                                logger.error("HVAC device error", error);
                            }
                    );

            logger.info("Awaiting termination signal: {}", name);
            sigTerm.await();

            logger.info("Received termination signal: {}", name);
            theEnd.dispose();

            logger.info("Shut down: {}", name);
            shutdownComplete.countDown();

        } catch (Throwable t) { // NOSONAR Consequences have been considered
            logger.fatal("Unexpected exception (" + name + ")");
        } finally {
            ThreadContext.pop();
        }
    }

    public Feed getFeed() {
        return feed;
    }

    @Override
    public void close() throws Exception {
        logger.warn("Shutting down: {}", getAddress());

        Flux
                .fromIterable(feed.sensorFlux2zone.values())
                .parallel()
                .runOn(Schedulers.boundedElastic())
                .doOnNext(z -> {
                    logger.info("shutting down zone: {}", z.getAddress());
                    try {
                        z.close();
                    } catch (Exception ex) {
                        logger.error("{}: failed to close(), nothing we can do now", z.getAddress(), ex);
                    }
                })
                .sequential()
                .blockLast();

        logger.info("Shut down: {}", getAddress());
    }

    public static class Feed {

        public final String unit;
        public final Map<Flux<Signal<Double, Void>>, Zone> sensorFlux2zone;
        public final Flux<Signal<ZoneStatus, String>> aggregateZoneFlux;
        public final Flux<Signal<UnitControlSignal, Void>> zoneControllerFlux;
        public final Flux<Signal<HvacCommand, Void>> unitControllerFlux;
        public final Flux<Signal<HvacDeviceStatus, Void>> hvacDeviceFlux;
        public final Flux<Map.Entry<String, Map.Entry<SchedulePeriod, ZoneSettings>>> scheduleFlux;

        public Feed(
                String unit,
                Map<Flux<Signal<Double, Void>>, Zone> sensorFlux2zone,
                Flux<Signal<ZoneStatus, String>> aggregateZoneFlux,
                Flux<Signal<UnitControlSignal, Void>> zoneControllerFlux,
                Flux<Signal<HvacCommand, Void>> unitControllerFlux,
                Flux<Signal<HvacDeviceStatus, Void>> hvacDeviceFlux, Flux<Map.Entry<String, Map.Entry<SchedulePeriod, ZoneSettings>>> scheduleFlux) {

            this.unit = unit;
            this.sensorFlux2zone = sensorFlux2zone;
            this.aggregateZoneFlux = aggregateZoneFlux;
            this.zoneControllerFlux = zoneControllerFlux;
            this.unitControllerFlux = unitControllerFlux;
            this.hvacDeviceFlux = hvacDeviceFlux;
            this.scheduleFlux = scheduleFlux;
        }
    }

    @Override
    public String toString() {
        return "UnitDirector(" + getAddress() + ")";
    }
}
