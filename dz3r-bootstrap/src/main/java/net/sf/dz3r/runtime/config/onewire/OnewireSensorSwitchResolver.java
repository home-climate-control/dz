package net.sf.dz3r.runtime.config.onewire;

import net.sf.dz3r.runtime.config.Id2Flux;
import net.sf.dz3r.runtime.config.SensorSwitchResolver;
import net.sf.dz3r.runtime.config.protocol.onewire.OnewireBusConfig;
import reactor.core.publisher.Flux;

import java.util.Set;

public class OnewireSensorSwitchResolver extends SensorSwitchResolver<OnewireBusConfig> {

    private OnewireSensorSwitchResolver(Set<OnewireBusConfig> source) {
        super(source);
    }

    @Override
    public Flux<Id2Flux> getSensorFluxes() {
        logger.error("NOT IMPLEMENTED: {}#getSensorFluxes()", getClass().getName());
        return Flux.empty();
    }
}
