package com.homeclimatecontrol.hcc.meta;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Data set sufficient to initialize a simple zone UI element.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record ZoneMeta(
        String id,
        String name,
        boolean hasEconomizer
) {

}
