package net.sf.dz3.runtime.config.quarkus;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

/**
 * Signal filter configuration.
 *
 */
public interface FilterConfig {
    @JsonProperty("id")
    String id();
    @JsonProperty("type")
    String type();
    @JsonProperty("sources")
    Set<String> sources();
}
