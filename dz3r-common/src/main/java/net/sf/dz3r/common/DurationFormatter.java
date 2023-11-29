package net.sf.dz3r.common;

import java.time.Duration;

public class DurationFormatter {
    public String format(long duration) {

        long withinDay = duration % Duration.ofDays(1).toMillis();

        if (withinDay == duration) {
            return format24H(duration);
        }

        return Duration.ofMillis(duration - withinDay).toDays() + "d"
                + (withinDay == 0 ? "" : " " + format24H(withinDay));
    }

    private String format24H(long duration) {
        // Longest time period Duration#toString() is aware of is hours, we need days
        return Duration.ofMillis(duration).toString().substring(2).replaceAll("(\\d[HMS])(?!$)", "$1 ").toLowerCase();
    }
}
