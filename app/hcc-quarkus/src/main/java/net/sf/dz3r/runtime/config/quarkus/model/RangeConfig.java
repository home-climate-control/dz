package net.sf.dz3r.runtime.config.quarkus.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface RangeConfig {
    @JsonProperty("min")
    Double min();
    @JsonProperty("max")
    Double max();
}
