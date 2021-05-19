package net.sf.dz3.scheduler;

import java.util.EmptyStackException;
import java.util.SortedMap;
import java.util.TreeMap;

import junit.framework.TestCase;
import net.sf.dz3.device.model.ZoneStatus;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

/**
 * 
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2018
 */
public class PeriodMatcherTest extends TestCase {
    
    private final Logger logger = LogManager.getLogger(getClass());

    public void testNone() {
        
        SortedMap<Period, ZoneStatus> zoneSchedule = new TreeMap<Period, ZoneStatus>();
        DateTime dt = new DateTime().withDate(2010, 1, 19).withHourOfDay(0).withMinuteOfHour(40);
        
        zoneSchedule.put(new Period("period", "0:15", "0:30", "......."), null);
        
        try {
            
            test(zoneSchedule, dt);
            fail("Should have thrown an exception by now");
            
        } catch (EmptyStackException ex) {
            
           logger.info("Got the exception ('we're fine)");
        }
        
    }
    
    public void testSimple() {
        
        SortedMap<Period, ZoneStatus> zoneSchedule = new TreeMap<Period, ZoneStatus>();
        
        Period p1 = new Period("period", "00:15", "00:30", ".......");
        zoneSchedule.put(p1, null);

        DateTime dt = new DateTime().withDate(2010, 1, 19).withHourOfDay(0);
        
        assertEquals("Improper match", p1, test(zoneSchedule, dt.withMinuteOfHour(20)));
    }

    public void testSimple2() {
        
        SortedMap<Period, ZoneStatus> zoneSchedule = new TreeMap<Period, ZoneStatus>();
        
        // Let's make sure that hours in Period.includes(long) are also properly converted
        Period p1 = new Period("period", "02:15", "02:30", ".......");
        zoneSchedule.put(p1, null);

        DateTime dt = new DateTime().withDate(2010, 1, 19).withHourOfDay(2);
        
        assertEquals("Improper match", p1, test(zoneSchedule, dt.withMinuteOfHour(20)));
    }

    public void testLadder() {
        
        SortedMap<Period, ZoneStatus> zoneSchedule = new TreeMap<Period, ZoneStatus>();
        
        Period p1 = new Period("period 1", "00:10", "00:30", ".......");
        Period p2 = new Period("period 2", "00:20", "00:40", ".......");
        Period p3 = new Period("period 3", "00:30", "00:50", ".......");

        zoneSchedule.put(p1, null);
        zoneSchedule.put(p2, null);
        zoneSchedule.put(p3, null);

        DateTime dt = new DateTime().withDate(2010, 1, 19).withHourOfDay(0);
        
        assertEquals("Improper match", p1, test(zoneSchedule, dt.withMinuteOfHour(15)));
        assertEquals("Improper match", p2, test(zoneSchedule, dt.withMinuteOfHour(25)));
        assertEquals("Improper match", p3, test(zoneSchedule, dt.withMinuteOfHour(35)));
        assertEquals("Improper match", p3, test(zoneSchedule, dt.withMinuteOfHour(45)));
    }

    public void testStack() {
        
        SortedMap<Period, ZoneStatus> zoneSchedule = new TreeMap<Period, ZoneStatus>();
        
        Period p1 = new Period("period 1", "00:10", "00:50", ".......");
        Period p2 = new Period("period 2", "00:15", "00:40", ".......");
        Period p3 = new Period("period 3", "00:20", "00:30", ".......");
        Period p4 = new Period("period 4", "01:00", "02:00", "       ");

        zoneSchedule.put(p1, null);
        zoneSchedule.put(p2, null);
        zoneSchedule.put(p3, null);
        zoneSchedule.put(p4, null);

        
        DateTime dt = new DateTime().withDate(2010, 1, 19).withHourOfDay(0);
        
        assertEquals("Improper match", p1, test(zoneSchedule, dt.withMinuteOfHour(12)));
        assertEquals("Improper match", p2, test(zoneSchedule, dt.withMinuteOfHour(18)));
        assertEquals("Improper match", p3, test(zoneSchedule, dt.withMinuteOfHour(22)));
        assertEquals("Improper match", p2, test(zoneSchedule, dt.withMinuteOfHour(32)));
        assertEquals("Improper match", p1, test(zoneSchedule, dt.withMinuteOfHour(42)));
    }

    private Period test(SortedMap<Period, ZoneStatus> zoneSchedule, DateTime time) {
        
        return new PeriodMatcher().match(zoneSchedule, time);
    }
}
