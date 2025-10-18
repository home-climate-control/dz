package com.homeclimatecontrol.hcc;

import java.time.Instant;
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
}
