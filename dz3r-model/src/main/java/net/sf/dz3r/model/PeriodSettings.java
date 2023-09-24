package net.sf.dz3r.model;

/**
 * Mapping between the schedule period and its settings, as defined by the schedule.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public record PeriodSettings(
        SchedulePeriod period,
        ZoneSettings settings
) {
}
