package net.sf.dz3.runtime.config;

import net.sf.dz3r.device.actuator.Switch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public abstract class ConfigurationContextAware {

    protected final Logger logger = LogManager.getLogger();
    protected final ConfigurationContext context;

    protected ConfigurationContextAware(ConfigurationContext context) {
        this.context = context;
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
            throw new IllegalArgumentException("Couldn't resolve switch for address=" + address);
        }

        logger.debug("getSwitch({}) = {}", address, result);
        return result;
    }
}
