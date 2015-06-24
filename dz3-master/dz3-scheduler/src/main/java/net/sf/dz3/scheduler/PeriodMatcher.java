package net.sf.dz3.scheduler;

import java.util.Iterator;
import java.util.SortedMap;
import java.util.Stack;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;
import org.joda.time.DateTime;

import net.sf.dz3.device.model.ZoneStatus;

/**
 * Utility class to determine the {@link Period} corresponding to time given.
 * 
 * Exists as a separate entity to enable unit testing.
 *  
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko</a> 2001-2015
 */
public class PeriodMatcher {

    private final Logger logger = Logger.getLogger(getClass());
    
    /**
     * Match a period against time given.
     * 
     * @param zoneSchedule Zone schedule.
     * @param time Time to match against.
     * 
     * @return Current period, or {@code null} if none was found.
     */
    public Period match(SortedMap<Period, ZoneStatus> zoneSchedule, DateTime time) {
        
        NDC.push("match");
        
        try {
        
            Stack<Period> stack = new Stack<Period>();
            
            logger.debug("Matching " + time);

            SortedMap<Period, ZoneStatus> today = getToday(zoneSchedule, time);

            for (Iterator<Period> i = today.keySet().iterator(); i.hasNext(); ) {

                Period p = i.next();

                if (p.includes(time)) {

                    logger.debug("Included " + p);
                    stack.push(p);
                }
            }

            return stack.pop();
            
        } finally {
            NDC.pop();
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
    private SortedMap<Period, ZoneStatus> getToday(SortedMap<Period, ZoneStatus> zoneSchedule, DateTime date) {
        
        NDC.push("getToday");
        
        try {
        
            SortedMap<Period, ZoneStatus> result = new TreeMap<Period, ZoneStatus>();

            for (Iterator<Period> i = zoneSchedule.keySet().iterator(); i.hasNext(); ) {

                Period p = i.next();

                if (p.includesDay(date)) {

                    logger.debug("Including " + p);
                    result.put(p, zoneSchedule.get(p));
                }
            }

            logger.debug(result.size() + " periods found");
            
            return result;
        
        } finally {
            NDC.pop();
        }
    }
}
