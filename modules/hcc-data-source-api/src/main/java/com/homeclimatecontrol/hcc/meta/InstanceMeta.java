package com.homeclimatecontrol.hcc.meta;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.UUID;

/**
 * Root of the HCC instance metadata.
 *
 * This data structure reflects the HCC configuration in a way suitable for external clients to build the UI
 * and start pulling the right data feeds.
 *
 * @param name Instance name, taken from {@code HccRawConfig#instance}.
 * @param id Persistent unique system ID.
 * @param configDigest Current parsed (not source) configuration message digest.
 * @param simple Simple UI metadata.
 * @param full Full control metadata.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record InstanceMeta(
        String name,
        UUID id,
        String configDigest,
        SimpleClientMeta simple,
        InstrumentClusterMeta full
) {
}
