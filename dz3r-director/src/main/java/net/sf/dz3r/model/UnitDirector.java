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
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * Assembles all the components related to one hardware HVAC unit, connects them, and manages their lifecycles.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class UnitDirector implements Addressable<String> {

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

        feed = connectFeeds(sensorFlux2zone, unitController, hvacDevice, hvacMode);

        Optional.ofNullable(scheduleUpdater).ifPresent(u -> connectScheduler(sensorFlux2zone.values(), u));
        Optional.ofNullable(metricsCollectorSet)
                .ifPresent(collectors -> Flux.fromIterable(collectors)
                        .doOnNext(c -> c.connect(feed))
                        .subscribeOn(Schedulers.boundedElastic())
                        .subscribe());
        Optional.ofNullable(connectorSet)
                .ifPresent(connectors -> Flux.fromIterable(connectors)
                        .doOnNext(c -> {
                            c.connect(feed);
                            // VT: FIXME: Connect the control input when the API signature is established
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .subscribe());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ThreadContext.push("shutdownHook");
            try {

                logger.warn("Received termination signal");
                sigTerm.countDown();
                logger.warn("Shutting down");
                try {
                    shutdownComplete.await();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    logger.error("Interrupted, can do nothing about it", ex);
                }
                logger.info("Shut down.");

            } finally {
                ThreadContext.pop();
            }
        }));

        logger.info("Configured");

        Flux.just(Instant.now())
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(this::start)
                .subscribe();
    }

    private Feed connectFeeds(
            Map<Flux<Signal<Double, Void>>, Zone> sensorFlux2zone,
            UnitController unitController,
            HvacDevice hvacDevice,
            HvacMode hvacMode) {

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
                hvacDeviceFlux
        );
    }

    private void connectScheduler(Collection<Zone> zones, ScheduleUpdater scheduleUpdater) {

        var name2zone = new TreeMap<String, Zone>();
        Flux.fromIterable(zones)
                .doOnNext(z -> name2zone.put(z.getAddress(), z))
                .blockLast();

        var scheduler = new Scheduler(name2zone);

        scheduler.connect(scheduleUpdater.update());
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

    private Map.Entry<Flux<Signal<Double, String>>, Zone> addZoneName(Map.Entry<Flux<Signal<Double, Void>>, Zone> kv) {
        var zoneName = kv.getValue().getAddress();
        return new AbstractMap.SimpleEntry<>(kv.getKey()
                .map(s -> new Signal<>(s.timestamp, s.getValue(), zoneName, s.status, s.error)), kv.getValue());
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

            logger.info("Starting the pipeline");
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

            logger.info("Awaiting termination signal");
            sigTerm.await();

            logger.info("Received termination signal");
            theEnd.dispose();

            logger.info("Shut down");
            shutdownComplete.countDown();

        } catch (Throwable t) { // NOSONAR Consequences have been considered
            logger.fatal("Unexpected exception");
        } finally {
            ThreadContext.pop();
        }
    }

    public Feed getFeed() {
        return feed;
    }

    public static class Feed {

        public final String unit;
        public final Map<Flux<Signal<Double, Void>>, Zone> sensorFlux2zone;
        public final Flux<Signal<ZoneStatus, String>> aggregateZoneFlux;
        public final Flux<Signal<UnitControlSignal, Void>> zoneControllerFlux;
        public final Flux<Signal<HvacCommand, Void>> unitControllerFlux;
        public final Flux<Signal<HvacDeviceStatus, Void>> hvacDeviceFlux;

        public Feed(
                String unit,
                Map<Flux<Signal<Double, Void>>, Zone> sensorFlux2zone,
                Flux<Signal<ZoneStatus, String>> aggregateZoneFlux,
                Flux<Signal<UnitControlSignal, Void>> zoneControllerFlux,
                Flux<Signal<HvacCommand, Void>> unitControllerFlux,
                Flux<Signal<HvacDeviceStatus, Void>> hvacDeviceFlux) {

            this.unit = unit;
            this.sensorFlux2zone = sensorFlux2zone;
            this.aggregateZoneFlux = aggregateZoneFlux;
            this.zoneControllerFlux = zoneControllerFlux;
            this.unitControllerFlux = unitControllerFlux;
            this.hvacDeviceFlux = hvacDeviceFlux;
        }
    }
}
