package net.sf.dz3r.runtime.config.filter;

/**
 * Median signal filter configuration.
 *
 * @param id Identifier, mandatory.
 * @param depth Median filter depth, mandatory.
 * @param source Signal source, mandatory.
 */
public record MedianFilterConfig(
        String id,
        Integer depth,
        String source
) {
}
