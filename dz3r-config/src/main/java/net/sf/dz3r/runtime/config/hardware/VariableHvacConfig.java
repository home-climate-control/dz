package net.sf.dz3r.runtime.config.hardware;

public record VariableHvacConfig(
        String id,
        String mode,
        String actuator,
        FilterConfig filter
) {
}
