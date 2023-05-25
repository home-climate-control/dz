package net.sf.dz3.runtime.config;

import java.util.List;

/**
 * Signal filter configuration.
 *
 * @param id Identifier, mandatory.
 * @param type Filter type. At this moment, two are available: {@code median-set}, and {@code median}
 * @param sources
 */
public record FilterConfig(
        String id,
        String type,
        List<String> sources
) {
}
