package com.homeclimatecontrol.hcc.meta;

/**
 * The top level metadata the client will see.
 *
 * @param type Endpoint type. Always {@link Type#DIRECT} for the actual HCC instance.
 * @param instance Instance metadata.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public record EndpointMeta(
        Type type,
        InstanceMeta instance
) {
    public enum Type {
        DIRECT,
        PROXY
    }
}
