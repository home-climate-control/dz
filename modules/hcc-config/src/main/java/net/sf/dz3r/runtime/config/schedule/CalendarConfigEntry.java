package net.sf.dz3r.runtime.config.schedule;

/**
 * Individual zone entry.
 *
 * This way of implementing it is more verbose than a mere string map at {@link ScheduleConfig}, but is less ambiguous.
 *
 * @param zone Zone to apply the schedule configuration to.
 * @param calendar Calendar name.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public record CalendarConfigEntry(
        String zone,
        String calendar
) {
}
