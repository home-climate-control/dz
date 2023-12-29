package net.sf.dz3r.runtime.config.quarkus.filter;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

/**
 * Median set filter configuration.
 *
 */
public interface MedianSetFilterConfig {
    @JsonProperty("id")
    String id();
    @JsonProperty("sources")
    Set<String> sources();
}
