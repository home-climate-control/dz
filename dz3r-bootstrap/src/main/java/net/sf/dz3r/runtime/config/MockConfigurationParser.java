package net.sf.dz3r.runtime.config;

import net.sf.dz3r.device.actuator.CqrsSwitch;
import net.sf.dz3r.device.actuator.NullCqrsSwitch;
import net.sf.dz3r.runtime.config.hardware.MockConfig;
import net.sf.dz3r.runtime.config.hardware.SwitchConfig;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class MockConfigurationParser extends ConfigurationContextAware {

    protected MockConfigurationParser(ConfigurationContext context) {
        super(context);
    }

    public Mono<List<CqrsSwitch>> parse(Set<MockConfig> source) {

        // Trivial operation, no need to bother with parallelizing
        return Flux
                .fromIterable(Optional.ofNullable(source).orElse(Set.of()))
                .flatMap(c -> Flux.fromIterable(c.switches()))
                .map(SwitchConfig::address)
                .map(NullCqrsSwitch::new)
                .doOnNext(s -> context.switches.register(s.getAddress(), s))
                .map(CqrsSwitch.class::cast)
                .collectList();
    }
}
