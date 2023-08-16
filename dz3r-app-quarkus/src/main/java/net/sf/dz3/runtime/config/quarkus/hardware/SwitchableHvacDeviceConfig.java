package net.sf.dz3.runtime.config.quarkus.hardware;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;

public interface SwitchableHvacDeviceConfig {
    @JsonProperty("id")
    String id();
    @JsonProperty("mode")
    String mode();
    @JsonProperty("switch-address")
    String switchAddress();
    @JsonProperty("switch-reverse")
    Optional<Boolean> switchReverse();
    @JsonProperty("filter")
    Optional<FilterConfig> filter();
}
