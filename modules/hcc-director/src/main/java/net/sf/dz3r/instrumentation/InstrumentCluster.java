package net.sf.dz3r.instrumentation;

import net.sf.dz3r.device.actuator.CqrsSwitch;
import net.sf.dz3r.device.actuator.HvacDevice;
import net.sf.dz3r.scheduler.ScheduleUpdater;
import com.homeclimatecontrol.hcc.signal.Signal;
import net.sf.dz3r.signal.health.SystemStatus;
import net.sf.dz3r.signal.hvac.HvacDeviceStatus;
import net.sf.dz3r.view.Connector;
import net.sf.dz3r.view.MetricsCollector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

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
    private final Flux<Map.Entry<String, CqrsSwitch<?>>> switches;
    private final Flux<Map.Entry<String, ScheduleUpdater>> schedule;
    private final Flux<Map.Entry<String, Connector>> connectors;
    private final Flux<Map.Entry<String, MetricsCollector>> collectors;
    private final Flux<Map.Entry<String, HvacDevice>> hvacDevices;

    private final Map<String, SensorStatusProcessor> sensorProcessors = new HashMap<>();
    private final Map<String, SwitchStatusProcessor> switchProcessors = new HashMap<>();

    /**
     * Status accumulator.
     *
     * This object gets updated and then emitted every time an update comes.
     */
    private final SystemStatus currentStatus = createEmptyStatus();

    private final Sinks.Many<Signal<SystemStatus, Void>> statusSink = Sinks.many().multicast().onBackpressureBuffer();

    public InstrumentCluster(
            Flux<Map.Entry<String, Flux<Signal<Double, Void>>>> sensors,
            Flux<Map.Entry<String, CqrsSwitch<?>>> switches,
            Flux<Map.Entry<String, ScheduleUpdater>> schedule,
            Flux<Map.Entry<String, Connector>> connectors,
            Flux<Map.Entry<String, MetricsCollector>> collectors,
            Flux<Map.Entry<String, HvacDevice>> hvacDevices
            ) {

        this.sensors = sensors;
        this.switches = switches;
        this.schedule = schedule;
        this.connectors = connectors;
        this.collectors = collectors;
        this.hvacDevices = hvacDevices;
    }

    /**
     * @return System status flux. A new item is emitted every time a particular entity's status is updated,
     * the item can and must be treated as an incremental update, though it may at times represent full system status.
     */
    public Flux<Signal<SystemStatus, Void>> getFlux() {

        connectSensors();
        connectSwitches();

        logger.error("FIXME: NOT IMPLEMENTED: getFlux(dampers)");
        logger.error("FIXME: NOT IMPLEMENTED: getFlux(schedule)");
        logger.error("FIXME: NOT IMPLEMENTED: getFlux(collectors)");
        logger.error("FIXME: NOT IMPLEMENTED: getFlux(connectors)");

        connectHvacDevices();

        return statusSink.asFlux();
    }

    private void connectSensors() {
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

                                logger.debug("update/sensor: id={}, status={}", id, s);

                                // Update the accumulated status
                                currentStatus.sensors().put(id, s);

                                // Send an incremental update
                                var incrementalStatus = createEmptyStatus();
                                incrementalStatus.sensors().put(id, s);

                                statusSink.tryEmitNext(new Signal<>(Instant.now(), incrementalStatus));
                            });

                });
    }

    private void connectSwitches() {

        switches
                .map(kv -> {
                    var id = kv.getKey();
                    var p = switchProcessors.computeIfAbsent(id, SwitchStatusProcessor::new);

                    return new AbstractMap.SimpleEntry<>(id, p.compute(kv.getValue().getFlux()));
                })
                .subscribe(kv -> {

                    String id = kv.getKey();
                    var status = kv.getValue();

                    status
                            .subscribe(s -> {

                                logger.debug("update/switch: id={}, status={}", id, s);

                                // Update the accumulated status
                                currentStatus.switches().put(id, s);

                                // Send an incremental update
                                var incrementalStatus = createEmptyStatus();
                                incrementalStatus.switches().put(id, s);

                                statusSink.tryEmitNext(new Signal<>(Instant.now(), incrementalStatus));
                            });
                });
    }

    private void connectHvacDevices() {

        // Unlike others, this status object gets passed directly without transformation

        hvacDevices

                // VT: FIXME: Ugly. This should not have been needed if the rest of the framework was put together correctly.
                // + bucket list for https://github.com/home-climate-control/dz/issues/271

                // PS: ... and it still doesn't work right, not all HVAC devices are displayed
                // but only those for which updates are being sent.

                .parallel()
                .runOn(Schedulers.boundedElastic())

                .map(kv -> new AbstractMap.SimpleEntry<>(kv.getKey(), kv.getValue().getFlux()))
                .subscribe(kv -> {

                    String id = kv.getKey();
                    var status = kv.getValue();

                    status
                            .subscribe(s -> {

                                logger.debug("update/hvacDevice: id={}, status={}", id, s);

                                // Update the accumulated status
                                currentStatus.hvacDevices().put(id, (Signal<HvacDeviceStatus, Void>) s);

                                // Send an incremental update
                                var incrementalStatus = createEmptyStatus();
                                incrementalStatus.hvacDevices().put(id, (Signal<HvacDeviceStatus, Void>) s);

                                statusSink.tryEmitNext(new Signal<>(Instant.now(), incrementalStatus));
                            });
                });

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
