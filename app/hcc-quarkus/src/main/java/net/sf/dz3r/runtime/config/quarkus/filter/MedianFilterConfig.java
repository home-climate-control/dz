package net.sf.dz3r.runtime.config.quarkus.filter;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Median filter configuration.
 *
 */
public interface MedianFilterConfig {
    @JsonProperty("id")
    String id();
    @JsonProperty("depth")
    Integer depth();
    @JsonProperty("source")
    String source();
}
