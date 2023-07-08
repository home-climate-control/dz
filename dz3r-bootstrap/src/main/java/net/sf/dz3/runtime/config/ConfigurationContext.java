package net.sf.dz3.runtime.config;

import net.sf.dz3r.device.actuator.HvacDevice;
import net.sf.dz3r.device.actuator.Switch;
import net.sf.dz3r.model.UnitController;
import net.sf.dz3r.model.UnitDirector;
import net.sf.dz3r.model.Zone;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.view.Connector;
import net.sf.dz3r.view.MetricsCollector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Common space for all the objects emitted and consumed by configurators.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class ConfigurationContext {

    protected final Logger logger = LogManager.getLogger();

    private final Map<String, Flux<Signal<Double, Void>>> sensors = new TreeMap<>();
    private final Map<Object, Switch<?>> switches = new LinkedHashMap<>();
    private final Map<String, Zone> zones = new TreeMap<>();
    private final Map<String, Connector> connectors = new TreeMap<>();
    private final Map<String, MetricsCollector> collectors = new TreeMap<>();
    private final Map<String, HvacDevice> hvacDevices = new TreeMap<>();
    private final Map<String, UnitController> units = new TreeMap<>();
    private final Map<String, UnitDirector> directors = new TreeMap<>();

    public void registerSensorFlux(Map.Entry<String, Flux<Signal<Double, Void>>> source) {
        logger.info("sensor available: {}", source.getKey());
        sensors.put(source.getKey(), source.getValue());
    }

    public void registerSwitch(Switch<?> s) {
        logger.info("switch available: {}", s.getAddress());
        switches.put(s.getAddress(), s);
    }

    public void registerZone(Zone zone) {
        logger.info("zone available: {}", zone.getAddress());
        zones.put(zone.getAddress(), zone);
    }

    public void registerConnector(String id, Connector c) {
        logger.info("connector available: {}", id);
        connectors.put(id, c);
    }

    public void registerCollector(String id, MetricsCollector c) {
        logger.info("collector available: {}", id);
        collectors.put(id, c);
    }

    public void registerHVAC(HvacDevice hvacDevice) {
        logger.info("HVAC device available: {}", hvacDevice.getAddress());
        hvacDevices.put(hvacDevice.getAddress(), hvacDevice);
    }

    public void registerUnit(UnitController unit) {
        logger.info("Unit controller available: {}", unit.getAddress());
        units.put(unit.getAddress(), unit);
    }

    public void registerDirector(UnitDirector d) {
        logger.info("Unit director available: {}", d.getAddress());
        directors.put(d.getAddress(), d);
    }
}
