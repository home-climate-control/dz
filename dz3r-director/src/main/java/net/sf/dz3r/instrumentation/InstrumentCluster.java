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

import java.util.Map;


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
     * the item can and must be treated as an incremental update.
     */
    public Flux<Signal<SystemStatus, Void>> getFlux() {

        logger.error("FIXME: NOT IMPLEMENTED: getFlux(SystemStatus)");

        return Flux.empty();
    }
}
