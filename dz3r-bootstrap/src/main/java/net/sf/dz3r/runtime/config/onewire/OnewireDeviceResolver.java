package net.sf.dz3r.runtime.config.onewire;

import net.sf.dz3r.runtime.config.DeviceResolver;
import net.sf.dz3r.runtime.config.Id2Flux;
import net.sf.dz3r.runtime.config.protocol.onewire.OnewireBusConfig;
import reactor.core.publisher.Flux;

import java.util.Set;

public class OnewireDeviceResolver extends DeviceResolver<OnewireBusConfig> {

    private OnewireDeviceResolver(Set<OnewireBusConfig> source) {
        super(source);
    }

    @Override
    public Flux<Id2Flux> getSensorFluxes() {
        logger.error("NOT IMPLEMENTED: {}#getSensorFluxes()", getClass().getName());
        return Flux.empty();
    }
}
