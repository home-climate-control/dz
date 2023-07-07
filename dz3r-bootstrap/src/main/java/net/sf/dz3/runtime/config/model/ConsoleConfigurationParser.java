package net.sf.dz3.runtime.config.model;

import net.sf.dz3.runtime.config.ConfigurationContext;
import net.sf.dz3.runtime.config.ConfigurationContextAware;
import net.sf.dz3r.view.swing.ReactiveConsole;

import java.util.Set;

public class ConsoleConfigurationParser extends ConfigurationContextAware {

    public ConsoleConfigurationParser(ConfigurationContext context) {
        super(context);
    }

    public ReactiveConsole parse(ConsoleConfig cf) {

        // VT: FIXME: Need the directors here, they're not yet exposed
        return new ReactiveConsole(Set.of());
    }
}
