package net.sf.dz3r.runtime.config.model;

public record PidControllerConfig(
        double p,
        double i,
        double d,
        double limit
) {
}
