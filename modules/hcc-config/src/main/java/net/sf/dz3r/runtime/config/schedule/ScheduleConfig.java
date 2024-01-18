package net.sf.dz3r.runtime.config.schedule;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Set;

/**
 * Calendar entries.
 *
 * @param googleCalendar Entries that belong to Google Calendar integration. There can be one integration only due to
 *               the way Google Calendar API is implemented.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record ScheduleConfig(
        Set<CalendarConfigEntry> googleCalendar
) {
}
