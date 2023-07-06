package net.sf.dz3.runtime.config.model;

import net.sf.dz3r.view.swing.ReactiveConsole;

import java.util.Set;

public class ConsoleConfigurationParser {
    public ReactiveConsole parse(ConsoleConfig cf) {

        // VT: FIXME: Need the directors here, they're not yet exposed
        return new ReactiveConsole(Set.of());
    }
}
