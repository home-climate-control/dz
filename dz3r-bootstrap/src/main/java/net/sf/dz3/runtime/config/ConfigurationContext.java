package net.sf.dz3.runtime.config;

import net.sf.dz3r.device.actuator.Switch;
import net.sf.dz3r.signal.Signal;
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

    public void registerSensorFlux(Map.Entry<String, Flux<Signal<Double, Void>>> source) {
        logger.info("sensor available: {}", source.getKey());
        sensors.put(source.getKey(), source.getValue());
    }

    public void registerSwitch(Switch<?> s) {
        logger.info("switch available: {}", s.getAddress());
        switches.put(s.getAddress(), s);

    }
}
