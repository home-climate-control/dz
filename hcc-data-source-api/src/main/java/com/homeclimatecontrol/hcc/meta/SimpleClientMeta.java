package com.homeclimatecontrol.hcc.meta;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Set;

/**
 * Data set sufficient to initialize a simple UI sufficient to control zones and units.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record SimpleClientMeta(
        Set<ZoneMeta> zones,
        Set<HvacDeviceMeta> devices
) {
}
