package com.homeclimatecontrol.hcc.model;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Defines a time period when a given {@link ZoneSettings} is to be activated.
 *
 * @param id Period ID. Doesn't participate in {@link #compareTo(SchedulePeriod)}, but does participate in {@link #equals(Object)} and {@link #hashCode()}.
 * @param name Period name. Has no significance other than for display and {@link #toString()}.
 * @param start Start time offset against midnight.
 * @param end End time offset against midnight.
 * @param days Days when this period is scheduled to be active, as a bitmask. 0x01 is Monday, 0x02 is Tuesday, and son on.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2024
 */
public record SchedulePeriod(
        String id,
        String name,
        LocalTime start,
        LocalTime end,
        byte days) implements Comparable<SchedulePeriod> {

    public boolean isSameDay() {
        return end.isAfter(start);
    }

    public boolean isAcrossMidnight() {
        return start.isAfter(end);
    }

    /**
     * Check if the given time is within this period.
     *
     * @param t Time of day to check for inclusion.
     * @return {@code true} if this period includes the time.
     */
    public boolean includes(LocalTime t) {

        return isSameDay()
                ? (start.equals(t) || start.isBefore(t)) && end.isAfter(t)
                : start.equals(t) || t.isAfter(start) || t.isBefore(end);
    }

    /**
     * Check if this period is active on the day of week of the given date.
     *
     * @param d Date to check for inclusion.
     * @return {@code true} if this period includes is active for the date's day of week..
     */
    public boolean includesDay(LocalDate d) {

        // Sunday based
        var day = d.getDayOfWeek().getValue();

        // Monday based
        day = (day + 6) % 7;

        return (days & 0xFF & (0x01 << day)) != 0;
    }

    @Override
    public int compareTo(SchedulePeriod o) {

        var diff = start.compareTo(o.start);

        if (diff != 0) {
            return diff;
        }

        // If two events start at the same time, the one that ends later is considered "less",
        // to allow easy overrides

        return o.end.compareTo(end);
    }

    @Override
    public boolean equals(Object o) {
        return o != null && toString().equals(o.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public String toString() {

        final var daysOfWeek = "MTWTFSS";

        var sb = new StringBuilder();

        sb.append(name).append(" (id=");
        sb.append(id).append(" ");
        sb.append(start);
        sb.append(" to ");
        sb.append(end);
        sb.append(" on ");

        for (var offset = 0; offset < 7; offset++) {

            int mask = 0x01 << offset;
            int set = days & mask;

            sb.append(set != 0 ? daysOfWeek.charAt(offset) : ".");
        }

        sb.append(")");

        return sb.toString();
    }
}
