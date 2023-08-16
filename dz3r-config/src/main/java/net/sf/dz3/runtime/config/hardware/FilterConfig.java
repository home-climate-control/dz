package net.sf.dz3.runtime.config.hardware;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.Duration;

/**
 * HVAC device air filter configuration.
 *
 * Do not confuse with {@link net.sf.dz3.runtime.config.filter.FilterConfig}.
 *
 * @param lifetime Time from the moment the filter is installed to the moment it needs to be replaced.
 */
@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record FilterConfig(
        Duration lifetime
) {
}
