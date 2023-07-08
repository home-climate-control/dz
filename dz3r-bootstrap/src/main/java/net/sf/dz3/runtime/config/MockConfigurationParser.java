package net.sf.dz3.runtime.config;

import net.sf.dz3.runtime.config.hardware.MockConfig;
import net.sf.dz3.runtime.config.hardware.SwitchConfig;
import net.sf.dz3r.device.actuator.NullSwitch;
import reactor.core.publisher.Flux;

import java.util.Set;

public class MockConfigurationParser extends ConfigurationContextAware {

    protected MockConfigurationParser(ConfigurationContext context) {
        super(context);
    }

    public void parse(Set<MockConfig> source) {

        Flux
                .fromIterable(source)
                .flatMap(c -> Flux.fromIterable(c.switches()))
                .map(SwitchConfig::address)
                .map(NullSwitch::new)
                .subscribe(s -> context.switches.register(s.getAddress(), s));
    }
}
