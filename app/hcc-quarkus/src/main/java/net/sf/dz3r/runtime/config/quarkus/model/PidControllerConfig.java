package net.sf.dz3r.runtime.config.quarkus.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface PidControllerConfig {
    @JsonProperty("p")
    double p();
    @JsonProperty("i")
    double i();
    @JsonProperty("d")
    default double d() { return 0; }
    @JsonProperty("limit")
    double limit();
}
