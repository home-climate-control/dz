package net.sf.dz3r.runtime.config.model;

import java.util.Set;

public record WebUiConfig(
        Integer port,
        Set<String> directors) {
}
