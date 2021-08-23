package net.sf.dz3r.model;

import net.sf.dz3r.device.actuator.HvacDevice;
import net.sf.dz3r.signal.HvacCommand;
import net.sf.dz3r.signal.HvacDeviceStatus;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.UnitControlSignal;
import net.sf.dz3r.signal.ZoneStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * Assembles all the components related to one hardware HVAC unit, connects them, and manages their lifecycles.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class UnitDirector {

    private final Logger logger = LogManager.getLogger();

    private final Flux<Signal<HvacDeviceStatus, Void>> hvacDeviceFlux;
    private final CountDownLatch sigTerm = new CountDownLatch(1);
    private final CountDownLatch shutdownComplete = new CountDownLatch(1);

    /**
     * Create an instance.
     *
     * @param sensorFlux2zone Mapping of sensor flux to zone it will feed.
     * @param unitController Unit controller for this set of zones.
     * @param hvacDevice HVAC device that serves this set of zones.
     * @param hvacMode HVAC device mode for this zone. {@link Thermostat} PID signals must have correct polarity for this mode.
     */
    public UnitDirector(
            Map<Flux<Signal<Double, Void>>, Zone> sensorFlux2zone,
            UnitController unitController,
            HvacDevice hvacDevice,
            HvacMode hvacMode
    ) {

        var aggregateZoneFlux = Flux
                .merge(extractSensorFluxes(sensorFlux2zone))
                .checkpoint("aggregate-sensor");
        var zoneControllerFlux = new ZoneController(sensorFlux2zone.values())
                .compute(aggregateZoneFlux)
                .checkpoint("zone-controller")
                .map(this::stripZoneName);
        var unitControllerFlux = unitController
                .compute(zoneControllerFlux)
                .checkpoint("unit-controller");
        hvacDeviceFlux = hvacDevice.compute(
                Flux.concat(
                        Flux.just(new Signal<>(Instant.now(), new HvacCommand(hvacMode, null, null))),
                        unitControllerFlux
                ));

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
        return hvacDeviceFlux;
    }

    public void run() {
        ThreadContext.push("run");
        try {

            logger.info("Starting the pipeline");
            var theEnd = hvacDeviceFlux
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
}
