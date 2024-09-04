package net.sf.dz3r.scheduler;

import com.homeclimatecontrol.hcc.model.ZoneSettings;
import net.sf.dz3r.model.SchedulePeriod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Utility class to determine the {@link SchedulePeriod} corresponding to time given.
 *
 * Exists as a separate entity to enable unit testing.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class SchedulePeriodMatcher {

    private final Logger logger = LogManager.getLogger(getClass());

    /**
     * Match a period against time given.
     *
     * @param zoneSchedule Zone schedule.
     * @param time Time to match against.
     *
     * @return Current period, or {@code null} if none was found.
     */
    public SchedulePeriod match(SortedMap<SchedulePeriod, com.homeclimatecontrol.hcc.model.ZoneSettings> zoneSchedule, LocalDateTime time) {

        ThreadContext.push("match");

        try {

            var stack = new ArrayDeque<SchedulePeriod>();

            logger.trace("matching {}", time);

            SortedMap<SchedulePeriod, com.homeclimatecontrol.hcc.model.ZoneSettings> today = getToday(zoneSchedule, time.toLocalDate());

            for (SchedulePeriod p : today.keySet()) {

                if (p.includes(time.toLocalTime())) {
                    logger.trace(p);
                    stack.push(p);
                }
            }

            try {
                return stack.pop();
            } catch (NoSuchElementException ex) {
                return null;
            }

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Filter out only those periods that correspond to the given date's day of week.
     *
     * @param zoneSchedule Period to zone status map.
     * @param date Date to match the day of week of.
     *
     * @return Map containing only periods for the given day of week.
     */
    private SortedMap<SchedulePeriod, ZoneSettings> getToday(SortedMap<SchedulePeriod, ZoneSettings> zoneSchedule, LocalDate date) {

        ThreadContext.push("getToday");

        try {

            var result = new TreeMap<SchedulePeriod, ZoneSettings>();

            for (var entry : zoneSchedule.entrySet()) {

                var p = entry.getKey();

                if (p.includesDay(date)) {
                    logger.trace(p);
                    result.put(p, entry.getValue());
                }
            }

            logger.debug("{} periods found", result.size());

            return result;

        } finally {
            ThreadContext.pop();
        }
    }
}
