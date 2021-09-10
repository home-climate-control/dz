package net.sf.dz3r.scheduler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Map;

/**
 * Defines a time period when a given {@link net.sf.dz3r.model.ZoneSettings} is to be activated.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class SchedulePeriod implements Comparable<SchedulePeriod> {

    private final Logger logger = LogManager.getLogger();

    private static final Map<Long, String> AM_PM_LOWERCASE = Map.of(0L, "am", 1L, "pm");
    private static final Map<Long, String> AM_PM_UPPERCASE = Map.of(0L, "AM", 1L, "PM");
    private static final String H_MM = "h:mm";
    private static final String HH_MM = "hh:mm";

    private static final List<DateTimeFormatter> timeFormats = List.of(
            DateTimeFormatter.ISO_LOCAL_TIME,
            new DateTimeFormatterBuilder()
                    .appendValue(ChronoField.HOUR_OF_DAY, 1)
                    .appendLiteral(':')
                    .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
                    .optionalStart()
                    .appendLiteral(':')
                    .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
                    .optionalStart()
                    .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
                    .toFormatter(),
            new DateTimeFormatterBuilder()
                    .appendValue(ChronoField.HOUR_OF_DAY, 2)
                    .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
                    .toFormatter(),
            new DateTimeFormatterBuilder()
                    .appendPattern(H_MM)
                    .appendText(ChronoField.AMPM_OF_DAY, AM_PM_LOWERCASE)
                    .toFormatter(),
            new DateTimeFormatterBuilder()
                    .appendPattern(H_MM)
                    .appendLiteral(' ')
                    .appendText(ChronoField.AMPM_OF_DAY, AM_PM_LOWERCASE)
                    .toFormatter(),
            new DateTimeFormatterBuilder()
                    .appendPattern(H_MM)
                    .appendText(ChronoField.AMPM_OF_DAY, AM_PM_UPPERCASE)
                    .toFormatter(),
            new DateTimeFormatterBuilder()
                    .appendPattern(H_MM)
                    .appendLiteral(' ')
                    .appendText(ChronoField.AMPM_OF_DAY, AM_PM_UPPERCASE)
                    .toFormatter(),
            new DateTimeFormatterBuilder()
                    .appendPattern(HH_MM)
                    .appendText(ChronoField.AMPM_OF_DAY, AM_PM_LOWERCASE)
                    .toFormatter(),
            new DateTimeFormatterBuilder()
                    .appendPattern(HH_MM)
                    .appendLiteral(' ')
                    .appendText(ChronoField.AMPM_OF_DAY, AM_PM_LOWERCASE)
                    .toFormatter(),
            new DateTimeFormatterBuilder()
                    .appendPattern(HH_MM)
                    .appendText(ChronoField.AMPM_OF_DAY, AM_PM_UPPERCASE)
                    .toFormatter(),
            new DateTimeFormatterBuilder()
                    .appendPattern(HH_MM)
                    .appendLiteral(' ')
                    .appendText(ChronoField.AMPM_OF_DAY, AM_PM_UPPERCASE)
                    .toFormatter()
    );

    /**
     * Period name.
     *
     * Has no significance other than for display and {@link #toString()}.
     */
    public final String name;

    /**
     * Start time offset against midnight.
     */
    public final LocalTime start;

    /**
     * End time offset against midnight.
     */
    public final LocalTime end;

    /**
     * Days when this period is scheduled to be active, as a bitmask.
     *
     * 0x01 is Monday, 0x02 is Tuesday, and son on.
     */
    public final byte days;

    /**
     * Create an instance from human readable arguments.
     *
     * @param name Period name.
     * @param startTime Start time in any reasonable format.
     * @param endTime End time in any reasonable format. Can't be equal to {@code startTime}, but can span across midnight.
     * @param days String consisting of seven characters, any non-space character is treated as a bit set,
     * space is treated as a bit cleared. Recommended characters would be corresponding day names, Monday
     * in the first position (at offset 0). If the period spans across midnight, the day is matched against the
     * start time only.
     */
    public SchedulePeriod(String name, String startTime, String endTime, String days) {

        if (name == null || "".equals(name)) {
            throw new IllegalArgumentException("Name can't be null or empty");
        }

        this.name = name;

        this.start = parseTime(startTime);
        this.end = parseTime(endTime);

        if (start.equals(end)) {
            throw new IllegalArgumentException("Start and end time are the same: " + start);
        }

        this.days = parseDays(days);
    }

    private LocalTime parseTime(String time) {

        ThreadContext.push("parse(" + time + ")");

        try {

            for (var format : timeFormats) {

                // This is happening rarely enough so we can afford do go the long way,
                // in interest of better debugging

                try {

                    return LocalTime.parse(time, format);

                } catch (DateTimeParseException ex) {

                    // VT: NOTE: Only uncomment this if you're having problems.
                    // This message may repeat over a hundred thousand times a day.

                    // logger.debug("Failed to parse '" + time + "' as '" + format + "'"); // NOSONAR Documentation
                }
            }

            // This is bad, none of available formats worked. Let's list them for the reference

            for (var format : timeFormats) {
                logger.error("Tried format: {}", format);
            }

            logger.error("Total of {} formats tried", timeFormats.size());

            throw new IllegalArgumentException("Tried all available formats (see the log for details) and failed, giving up");

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Convert the string given to the constructor into a bit mask.
     *
     * @param days String to convert.
     *
     * @return Bit mask to use as {@link #days}.
     */
    private byte parseDays(String days) {

        if (days == null || days.length() != 7) {

            throw new IllegalArgumentException("Days argument malformed, see source code for instructions");
        }

        byte result = 0x00;

        for (var offset = 0; offset < 7; offset++) {

            byte mask = (byte)(0x01 << offset);
            var c = days.charAt(offset);

            if (c != ' ') {
                result = (byte)(result | mask);
            }
        }

        return result;
    }

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
     *
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
     *
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

        sb.append(name).append(" (");
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
