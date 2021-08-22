package net.sf.dz3r.model;

import net.sf.dz3r.device.actuator.HvacDevice;
import net.sf.dz3r.signal.HvacCommand;
import net.sf.dz3r.signal.HvacDeviceStatus;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.UnitControlSignal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.AbstractMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Assembles all the components related to one hardware HVAC unit, connects them, and manages their lifecycles.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class UnitDirector {

    private final Logger logger = LogManager.getLogger();

    private final Flux<Signal<HvacDeviceStatus, Void>> hvacDeviceFlux;

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

        var aggregateZoneFlux = Flux.merge(
                sensorFlux2zone
                .entrySet()
                .stream()
                .map(this::addZoneName)
                .map(kv -> kv.getValue().compute(kv.getKey()))
                .collect(Collectors.toSet()));

        var zoneController = new ZoneController(new TreeSet<>(sensorFlux2zone.values()));
        hvacDeviceFlux = hvacDevice.compute(
                Flux.concat(
                        Flux.just(new Signal<>(Instant.now(), new HvacCommand(hvacMode, null, null))),
                        unitController.compute(stripZoneName(zoneController.compute(aggregateZoneFlux)))
                ));

        new Thread(() -> {

            logger.info("Starting the pipeline");
            var theEnd = hvacDeviceFlux
                    .doOnNext(s -> logger.debug("HVAC device: {}", s))
                    .blockLast();
            logger.info("Complete: {}", theEnd);

        }).start();

        logger.info("Configured");
    }

    private Map.Entry<Flux<Signal<Double, String>>, Zone> addZoneName(Map.Entry<Flux<Signal<Double, Void>>, Zone> kv) {
        var zoneName = kv.getValue().getAddress();
        return new AbstractMap.SimpleEntry<>(kv.getKey()
                .map(s -> new Signal<>(s.timestamp, s.getValue(), zoneName, s.status, s.error)), kv.getValue());
    }

    private Flux<Signal<UnitControlSignal, Void>> stripZoneName(Flux<Signal<UnitControlSignal, String>> source) {
        return source.map(s -> new Signal<>(s.timestamp, s.getValue(), null, s.status, s.error));
    }

    public final Flux<Signal<HvacDeviceStatus, Void>> getFlux() {
        return hvacDeviceFlux;
    }
}
