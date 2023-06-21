package net.sf.dz3.runtime.config.quarkus.hardware;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface MultiStageUnitControllerConfig {
    @JsonProperty("id")
    String id();
}
