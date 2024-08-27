package com.homeclimatecontrol.hcc.meta;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * The top level metadata the client will see.
 *
 * @param protocolVersion Version for the clients to determine compatibility.
 * @param type Endpoint type. Always {@link Type#DIRECT} for the actual HCC instance.
 * @param instance Instance metadata.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record EndpointMeta(
        String protocolVersion,
        Type type,
        InstanceMeta instance
) {
    public enum Type {
        DIRECT,
        PROXY
    }
}
