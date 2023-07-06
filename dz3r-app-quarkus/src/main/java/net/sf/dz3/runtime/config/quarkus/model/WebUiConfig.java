package net.sf.dz3.runtime.config.quarkus.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;
import java.util.Set;

public interface WebUiConfig {

    @JsonProperty("port")
    Optional<Integer> port();

    @JsonProperty("directors")
    Set<String> directors();
}
