package net.sf.dz3.runtime.config.quarkus.filter;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

/**
 * Signal filter configuration.
 *
 */
public interface FilterConfig {

    @JsonProperty("median")
    Set<MedianFilterConfig> median();

    @JsonProperty("median-set")
    Set<MedianSetFilterConfig> medianSet();
}
