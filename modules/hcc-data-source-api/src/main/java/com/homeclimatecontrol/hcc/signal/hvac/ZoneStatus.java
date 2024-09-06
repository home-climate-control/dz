package com.homeclimatecontrol.hcc.signal.hvac;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.homeclimatecontrol.hcc.model.PeriodSettings;
import com.homeclimatecontrol.hcc.model.ZoneSettings;

/**
 * Zone status.
 *
 * This object defines the actual zone status in real time.
 *
 * @param settings Zone settings derived from {@link Zone#settings}.
 * @param callingStatus Calling status as provided by the pipeline.
 * @param economizerStatus Economizer status as provided by the pipeline.
 * @param periodSettings Zone period settings derived from {@link Zone#periodSettings}.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2024
 */
@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record ZoneStatus(
        ZoneSettings settings,
        CallingStatus callingStatus,
        EconomizerStatus economizerStatus,
        PeriodSettings periodSettings
) {
}