package net.sf.dz3.runtime.config;

import net.sf.dz3r.device.actuator.Switch;
import net.sf.dz3r.signal.Signal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;

import java.util.Map;

public abstract class ConfigurationContextAware {

    protected final Logger logger = LogManager.getLogger();
    protected final ConfigurationContext context;

    protected ConfigurationContextAware(ConfigurationContext context) {
        this.context = context;
    }

    protected final Flux<Signal<Double, Void>> getSensor(String address) {
        var result = context
                .sensors
                .getFlux()
                .filter(s -> s.getKey().equals(address))
                .map(Map.Entry::getValue)
                .take(1)
                .blockFirst();

        if (result == null) {
            throw new IllegalArgumentException("Couldn't resolve sensor flux for id or address '" + address + "'");
        }

        logger.debug("getSensor({}) = {}", address, result);
        return result;
    }

    protected final Switch<?> getSwitch(String address) {
        var result = context
                .switches
                .getFlux()
                .filter(s -> s.getKey().equals(address))
                .map(Map.Entry::getValue)
                .take(1)
                .blockFirst();

        if (result == null) {
            throw new IllegalArgumentException("Couldn't resolve switch for id or address '" + address + "'");
        }

        logger.debug("getSwitch({}) = {}", address, result);
        return result;
    }
}
