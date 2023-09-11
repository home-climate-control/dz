package net.sf.dz3r.runtime.config.quarkus.hardware;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface SingleStageUnitControllerConfig {
    @JsonProperty("id")
    String id();
}
