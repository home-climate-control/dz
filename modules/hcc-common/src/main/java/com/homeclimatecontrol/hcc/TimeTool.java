package com.homeclimatecontrol.hcc;

import java.time.Instant;
import java.util.TimeZone;

public class TimeTool {

    /**
     * @return Instant at midnight of the current day.
     */
    public static Instant atMidnightUTC() {
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
