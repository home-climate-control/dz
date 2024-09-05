package com.homeclimatecontrol.hcc;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.homeclimatecontrol.hcc.meta.EndpointMeta;
import com.homeclimatecontrol.hcc.signal.Signal;
import com.homeclimatecontrol.hcc.signal.hvac.ZoneStatus;

import java.util.Map;

/**
 * A combined packet containing everything the client application needs to initialize the UI and populate it with data.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2024
 */
@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record ClientBootstrap(
        EndpointMeta meta,
        Map<String, Signal<ZoneStatus, String>> zoneMap
) {
}
