package com.homeclimatecontrol.hcc;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.TimeZone;

/**
 * Time manipulation utilities.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2025
 */
public class TimeTool {

    /**
     * Get the day start instant.
     *
     * @return Instant at midnight at the beginning of the current day.
     */
    public static Instant atDayStartUTC() {
        var now = Instant.now();
        var dt = now.atZone(TimeZone.getTimeZone("UTC").toZoneId());

        return dt
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .toInstant();
    }

    /**
     * Parse an ISO_8601 or ISO_8601_local formatted time string.
     *
     * @param source String to parse.
     *
     * @return either the parsed time, or {@code null} if the string is null or blank.
     * @throws DateTimeParseException if the string can't be parsed.
     */
    public static Instant parseISO_8601(String source) {

        if (source == null || source.isBlank()) {
            return null;
        }

        return OffsetDateTime.parse(source).toInstant();
    }
}
