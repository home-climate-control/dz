package net.sf.dz3.runtime.config.onewire;

import net.sf.dz3.runtime.config.SensorSwitchResolver;
import net.sf.dz3.runtime.config.protocol.onewire.OnewireBusConfig;
import net.sf.dz3r.signal.Signal;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Set;

public class OnewireSensorSwitchResolver extends SensorSwitchResolver<OnewireBusConfig> {

    private OnewireSensorSwitchResolver(Set<OnewireBusConfig> source) {
        super(source);
    }

    @Override
    protected Map<String, Flux<Signal<Double, Void>>> getSensorFluxes(Set<OnewireBusConfig> source) {
        return Map.of();
    }
}
