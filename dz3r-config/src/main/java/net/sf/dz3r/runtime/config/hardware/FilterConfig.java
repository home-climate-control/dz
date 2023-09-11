package net.sf.dz3r.runtime.config.hardware;

import java.time.Duration;

/**
 * HVAC device air filter configuration.
 *
 * Do not confuse with {@link net.sf.dz3r.runtime.config.filter.FilterConfig}.
 *
 * @param lifetime Time from the moment the filter is installed to the moment it needs to be replaced.
 */
public record FilterConfig(
        Duration lifetime
) {
}
