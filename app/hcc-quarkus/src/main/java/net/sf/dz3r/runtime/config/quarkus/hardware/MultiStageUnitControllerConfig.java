package net.sf.dz3r.runtime.config.quarkus.hardware;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public interface MultiStageUnitControllerConfig {
    @JsonProperty("id")
    String id();

    @JsonProperty("stages")
    List<Double> stages();
}
