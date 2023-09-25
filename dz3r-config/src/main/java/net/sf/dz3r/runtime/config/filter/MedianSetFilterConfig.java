package net.sf.dz3r.runtime.config.filter;

import java.util.Set;

/**
 * Signal filter configuration.
 *
 * @param id Identifier, mandatory.
 * @param sources Signal source address set, mandatory.
 */
public record MedianSetFilterConfig(
        String id,
        Set<String> sources
) {
}
