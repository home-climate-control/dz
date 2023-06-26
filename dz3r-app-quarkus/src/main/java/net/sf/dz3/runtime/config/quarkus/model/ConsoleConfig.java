package net.sf.dz3.runtime.config.quarkus.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public interface ConsoleConfig {
    @JsonProperty("directors")
    Set<String> directors();
}
