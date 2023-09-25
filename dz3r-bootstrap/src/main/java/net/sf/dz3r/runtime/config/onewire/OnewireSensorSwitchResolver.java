package net.sf.dz3r.runtime.config.onewire;

import net.sf.dz3r.runtime.config.SensorSwitchResolver;
import net.sf.dz3r.runtime.config.protocol.onewire.OnewireBusConfig;
import net.sf.dz3r.signal.Signal;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Set;

public class OnewireSensorSwitchResolver extends SensorSwitchResolver<OnewireBusConfig> {

    private OnewireSensorSwitchResolver(Set<OnewireBusConfig> source) {
        super(source);
    }

    @Override
    public Flux<Map.Entry<String, Flux<Signal<Double, Void>>>> getSensorFluxes() {
        logger.error("NOT IMPLEMENTED: {}#getSensorFluxes()", getClass().getName());
        return Flux.empty();
    }
}
