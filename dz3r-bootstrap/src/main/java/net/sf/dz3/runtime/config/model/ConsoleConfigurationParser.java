package net.sf.dz3.runtime.config.model;

import net.sf.dz3.runtime.config.ConfigurationContext;
import net.sf.dz3.runtime.config.ConfigurationContextAware;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.view.swing.ReactiveConsole;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ConsoleConfigurationParser extends ConfigurationContextAware {

    public ConsoleConfigurationParser(ConfigurationContext context) {
        super(context);
    }

    public ReactiveConsole parse(ConsoleConfig cf) {

        var directors = context
                .directors
                .getFlux()
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet())
                .block();

        var sensors = context
                .sensors
                .getFlux()
                .filter(s -> isConfigured(cf.sensors(), s))
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .block();

        // VT: NOTE: next step - just remove block() and make the constructor consume the fluxes, it iterates through them anyway

        return new ReactiveConsole(directors, sensors, Optional.ofNullable(cf.units()).orElse(TemperatureUnit.C));
    }

    private boolean isConfigured(Set<String> sensors, Map.Entry<String, Flux<Signal<Double, Void>>> s) {
        return sensors.contains(s.getKey());
    }
}
