package net.sf.dz3.runtime.config.model;

public record PidControllerConfig(
        double p,
        double i,
        double d,
        double limit
) {
}
