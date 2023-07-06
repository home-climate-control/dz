package net.sf.dz3.runtime.config;

import net.sf.dz3.runtime.config.hardware.MockConfig;
import net.sf.dz3r.device.actuator.NullSwitch;
import net.sf.dz3r.device.actuator.Switch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;

import java.util.Set;

public class MockConfigurationParser {

    private final Logger logger = LogManager.getLogger();

    public void parse(Set<MockConfig> source) {

        Flux
                .fromIterable(source)
                .flatMap(c -> Flux.fromIterable(c.switches()))
                .map(c -> {
                    return new NullSwitch(c.address());
                })
                .doOnNext(s -> logger.debug("mock switch: {}", s.getAddress()))
                .map(Switch::getFlux)
                .blockLast();
    }
}
