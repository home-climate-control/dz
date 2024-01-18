package net.sf.dz3r.runtime.config.model;

import net.sf.dz3r.instrumentation.InstrumentCluster;
import net.sf.dz3r.runtime.config.ConfigurationContext;
import net.sf.dz3r.runtime.config.ConfigurationContextAware;
import net.sf.dz3r.view.swing.ReactiveConsole;

import java.util.Map;
import java.util.Optional;
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
                .filter(d -> isConfigured("console.directors", cf.directors(), d))
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet())
                .block();

        var sensors = context
                .sensors
                .getFlux()
                .filter(s -> isConfigured("console.sensors", cf.sensors(), s))
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .block();

        // VT: NOTE: next step - just remove block() and make the constructor consume the fluxes, it iterates through them anyway

        try {
            return new ReactiveConsole(instance, directors, sensors, ic, Optional.ofNullable(cf.units()).orElse(TemperatureUnit.C));
        } catch (UnsatisfiedLinkError ex) {
            // Not really an actionable message, more like noise. No big deal, we just skip instantiation
            logger.error("Did you by chance configure the Swing Console for headless environment? See debug log for details");
            logger.debug("Original exception trace", ex);
            return null;
        }
    }
}
