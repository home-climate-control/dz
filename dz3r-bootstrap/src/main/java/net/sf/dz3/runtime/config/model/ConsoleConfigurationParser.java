package net.sf.dz3.runtime.config.model;

import net.sf.dz3.runtime.config.ConfigurationContext;
import net.sf.dz3.runtime.config.ConfigurationContextAware;
import net.sf.dz3r.instrumentation.InstrumentCluster;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.view.swing.ReactiveConsole;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ConsoleConfigurationParser extends ConfigurationContextAware {

    private final InstrumentCluster ic;

    public ConsoleConfigurationParser(ConfigurationContext context, InstrumentCluster ic) {
        super(context);

        this.ic = ic;
    }

    public ReactiveConsole parse(String instance, ConsoleConfig cf) {

        if (cf == null) {
            logger.warn("Console is not configured");
            return null;
        }

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

        return new ReactiveConsole(instance, directors, sensors, ic, Optional.ofNullable(cf.units()).orElse(TemperatureUnit.C));
    }

    private boolean isConfigured(Set<String> sensors, Map.Entry<String, Flux<Signal<Double, Void>>> s) {
        return sensors.contains(s.getKey());
    }
}
