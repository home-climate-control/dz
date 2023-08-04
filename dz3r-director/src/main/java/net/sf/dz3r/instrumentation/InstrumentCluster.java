package net.sf.dz3r.instrumentation;

import net.sf.dz3r.device.actuator.HvacDevice;
import net.sf.dz3r.device.actuator.Switch;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.health.SystemStatus;
import net.sf.dz3r.view.Connector;
import net.sf.dz3r.view.MetricsCollector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Instant;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;


/**
 * Collection of all raw signal processors emitting a coherent "at a glance" system status.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2023
 */
public class InstrumentCluster {

    private final Logger logger = LogManager.getLogger();

    private final Flux<Map.Entry<String, Flux<Signal<Double, Void>>>> sensors;
    private final Flux<Map.Entry<String, Switch<?>>> switches;
    private final Flux<Map.Entry<String, Connector>> connectors;
    private final Flux<Map.Entry<String, MetricsCollector>> collectors;
    private final Flux<Map.Entry<String, HvacDevice>> hvacDevices;

    private final Map<String, SensorStatusProcessor> sensorProcessors = new HashMap<>();

    /**
     * Status accumulator.
     *
     * This object gets updated and then emitted every time an update comes.
     */
    private final SystemStatus currentStatus = createEmptyStatus();

    private final Sinks.Many<Signal<SystemStatus, Void>> statusSink = Sinks.many().multicast().onBackpressureBuffer();

    public InstrumentCluster(
            Flux<Map.Entry<String, Flux<Signal<Double, Void>>>> sensors,
            Flux<Map.Entry<String, Switch<?>>> switches,
            Flux<Map.Entry<String, Connector>> connectors,
            Flux<Map.Entry<String, MetricsCollector>> collectors,
            Flux<Map.Entry<String, HvacDevice>> hvacDevices
            ) {

        this.sensors = sensors;
        this.switches = switches;
        this.connectors = connectors;
        this.collectors = collectors;
        this.hvacDevices = hvacDevices;
    }

    /**
     * @return System status flux. A new item is emitted every time a particular entity's status is updated,
     * the item can and must be treated as an incremental update, though it may at times represent full system status.
     */
    public Flux<Signal<SystemStatus, Void>> getFlux() {

        sensors
                .map(kv -> {
                    var id = kv.getKey();
                    var p = sensorProcessors.computeIfAbsent(id, SensorStatusProcessor::new);

                    return new AbstractMap.SimpleEntry<>(id, p.compute(kv.getValue()));
                })
                .subscribe(kv -> {

                    String id = kv.getKey();
                    var status = kv.getValue();

                    status
                            .subscribe(s -> {

                                logger.info("update: id={}, status={}", id, s);

                                // Update the accumulated status
                                currentStatus.sensors().put(id, s);

                                // Send an incremental update
                                var incrementalStatus = createEmptyStatus();
                                incrementalStatus.sensors().put(id, s);

                                statusSink.tryEmitNext(new Signal<>(Instant.now(), incrementalStatus));
                            });

                });

        logger.error("FIXME: NOT IMPLEMENTED: getFlux(SystemStatus)");

        return statusSink.asFlux();
    }

    private SystemStatus createEmptyStatus() {
        return new SystemStatus(
                new TreeMap<>(),
                new TreeMap<>(),
                new TreeMap<>(),
                new TreeMap<>(),
                new TreeMap<>());
    }
}
