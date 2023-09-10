package net.sf.dz3r.runtime.config.filter;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Set;

/**
 * Signal filter configuration.
 *
 *  Do not confuse with {@link net.sf.dz3r.runtime.config.hardware.FilterConfig}.
 *
 * @param median Set of median filters.
 * @param medianSet Set of median set filters.
 */
@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record FilterConfig(

        Set<MedianFilterConfig> median,
        Set<MedianSetFilterConfig> medianSet
) {
}
