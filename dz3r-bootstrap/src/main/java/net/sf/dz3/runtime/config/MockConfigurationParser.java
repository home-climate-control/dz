package net.sf.dz3.runtime.config;

import net.sf.dz3.runtime.config.hardware.MockConfig;
import net.sf.dz3.runtime.config.hardware.SwitchConfig;
import net.sf.dz3r.device.actuator.NullSwitch;
import net.sf.dz3r.device.actuator.Switch;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class MockConfigurationParser extends ConfigurationContextAware {

    protected MockConfigurationParser(ConfigurationContext context) {
        super(context);
    }

    public Mono<List<Switch>> parse(Set<MockConfig> source) {

        // Trivial operation, no need to bother with parallelizing
        return Flux
                .fromIterable(Optional.ofNullable(source).orElse(Set.of()))
                .flatMap(c -> Flux.fromIterable(c.switches()))
                .map(SwitchConfig::address)
                .map(NullSwitch::new)
                .doOnNext(s -> context.switches.register(s.getAddress(), s))
                .map(Switch.class::cast)
                .collectList();
    }
}
