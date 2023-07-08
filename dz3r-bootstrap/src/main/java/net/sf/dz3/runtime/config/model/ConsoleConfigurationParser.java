package net.sf.dz3.runtime.config.model;

import net.sf.dz3.runtime.config.ConfigurationContext;
import net.sf.dz3.runtime.config.ConfigurationContextAware;
import net.sf.dz3r.view.swing.ReactiveConsole;

import java.util.Map;
import java.util.Optional;
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
                .map(Object.class::cast)
                .collect(Collectors.toSet())
                .block();

        return new ReactiveConsole(directors, Optional.ofNullable(cf.units()).orElse(TemperatureUnit.C));
    }
}
