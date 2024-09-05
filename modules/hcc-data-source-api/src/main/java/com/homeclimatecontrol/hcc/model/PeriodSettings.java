package com.homeclimatecontrol.hcc.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Mapping between the schedule period and its settings, as defined by the schedule.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2024
 */
@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record PeriodSettings(
        SchedulePeriod period,
        ZoneSettings settings
) {
}
