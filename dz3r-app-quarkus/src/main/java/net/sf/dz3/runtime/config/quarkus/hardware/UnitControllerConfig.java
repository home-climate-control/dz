package net.sf.dz3.runtime.config.quarkus.hardware;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public interface UnitControllerConfig {
    @JsonProperty("single-stage")
    Set<SingleStageUnitControllerConfig> singleStage();
    @JsonProperty("multi-stage")
    Set<MultiStageUnitControllerConfig> multiStage();
}
