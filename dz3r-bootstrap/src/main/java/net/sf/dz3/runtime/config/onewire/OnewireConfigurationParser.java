package net.sf.dz3.runtime.config.onewire;

import net.sf.dz3.runtime.config.ConfigurationContext;
import net.sf.dz3.runtime.config.ConfigurationContextAware;
import net.sf.dz3.runtime.config.protocol.onewire.OnewireBusConfig;
import reactor.core.publisher.Mono;

import java.util.Set;

public class OnewireConfigurationParser extends ConfigurationContextAware {

    public OnewireConfigurationParser(ConfigurationContext context) {
        super(context);
    }

    public Mono<Void> parse(Set<OnewireBusConfig> source) {
        logger.error("FIXME: NOT IMPLEMENTED: 1-Wire config parser");
        return Mono.empty();
    }
}
