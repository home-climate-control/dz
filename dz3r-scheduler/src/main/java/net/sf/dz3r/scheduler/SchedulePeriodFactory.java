package net.sf.dz3r.scheduler;

import net.sf.dz3r.model.SchedulePeriod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds {@link SchedulePeriod} instances.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class SchedulePeriodFactory {

    private final Logger logger = LogManager.getLogger();

    private static final Map<Long, String> AM_PM_LOWERCASE = Map.of(0L, "am", 1L, "pm");
    private static final Map<Long, String> AM_PM_UPPERCASE = Map.of(0L, "AM", 1L, "PM");
    private static final String H_MM = "h:mm";
    private static final String HH_MM = "hh:mm";

    private static final List<DateTimeFormatter> timeFormats = List.of(
            DateTimeFormatter.ISO_LOCAL_TIME,
            DateTimeFormatter.ISO_DATE_TIME,
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
     * Create an instance with a random {@link SchedulePeriod#id}.
     */
    SchedulePeriod build(String name, String startTime, String endTime, String days) {
        return build(UUID.randomUUID().toString(), name, startTime, endTime, days);
    }

    /**
     * Create an instance from human readable arguments.
     *
     * @param id Period id.
     * @param name Period name.
     * @param startTime Start time in any reasonable format.
     * @param endTime End time in any reasonable format. Can't be equal to {@code startTime}, but can span across midnight.
     * @param days String consisting of seven characters, any non-space character is treated as a bit set,
     * space is treated as a bit cleared. Recommended characters would be corresponding day names, Monday
     * in the first position (at offset 0). If the period spans across midnight, the day is matched against the
     * start time only.
     */
    public SchedulePeriod build(String id, String name, String startTime, String endTime, String days) {

        if (name == null || "".equals(name)) {
            throw new IllegalArgumentException("Name can't be null or empty");
        }

        var start = parseTime(startTime);
        var end = parseTime(endTime);

        return build(id, name, start, end, days);
    }

    /**
     * Create an instance from human readable arguments.
     *
     * @param id Period id.
     * @param name Period name.
     * @param start Start time.
     * @param end End time. Can't be equal to {@code startTime}, but can span across midnight.
     * @param days String consisting of seven characters, any non-space character is treated as a bit set,
     * space is treated as a bit cleared. Recommended characters would be corresponding day names, Monday
     * in the first position (at offset 0). If the period spans across midnight, the day is matched against the
     * start time only.
     */
    public SchedulePeriod build(String id, String name, LocalTime start, LocalTime end, String days) {

        if (start.equals(end)) {
            throw new IllegalArgumentException("Start and end time are the same: " + start);
        }

        var daysBitmask = parseDays(days);

        return new SchedulePeriod(id, name, start, end, daysBitmask);
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

            throw new IllegalArgumentException("Tried all available formats to parse '" + time + "' (see the log for details) and failed, giving up");

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Convert the string given to the constructor into a bit mask.
     *
     * @param days String to convert.
     *
     * @return Bit mask to use as {@link SchedulePeriod#days}.
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
}
