package net.sf.dz3.scheduler;

import java.util.Calendar;
import java.util.Date;
import java.util.EmptyStackException;
import java.util.GregorianCalendar;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import net.sf.dz3.device.model.ZoneStatus;

import junit.framework.TestCase;

/**
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko</a> 2001-2010
 */
public class PeriodMatcherTest extends TestCase {
    
    private final Logger logger = Logger.getLogger(getClass());

    public void testNone() {
        
        SortedMap<Period, ZoneStatus> zoneSchedule = new TreeMap<Period, ZoneStatus>();
        
        Calendar cal = new GregorianCalendar();
        
        cal.set(2010, 0, 19, 0, 40);
        zoneSchedule.put(new Period("period", "0:15", "0:30", "......."), null);
        
        try {
            
            test(zoneSchedule, cal.getTime());
            fail("Should have thrown an exception by now");
            
        } catch (EmptyStackException ex) {
            
           logger.info("Got the exception ('we're fine)");
        }
        
    }
    
    public void testSimple() {
        
        SortedMap<Period, ZoneStatus> zoneSchedule = new TreeMap<Period, ZoneStatus>();
        
        Period p1 = new Period("period", "00:15", "00:30", ".......");
        zoneSchedule.put(p1, null);

        
        Calendar cal = new GregorianCalendar();
        
        cal.set(2010, 0, 19, 0, 20);
        
        assertEquals("Improper match", p1, test(zoneSchedule, cal.getTime()));
    }

    public void testSimple2() {
        
        SortedMap<Period, ZoneStatus> zoneSchedule = new TreeMap<Period, ZoneStatus>();
        
        // Let's make sure that hours in Period.includes(long) are also properly converted
        Period p1 = new Period("period", "02:15", "02:30", ".......");
        zoneSchedule.put(p1, null);

        
        Calendar cal = new GregorianCalendar();
        
        cal.set(2010, 0, 19, 2, 20);
        
        assertEquals("Improper match", p1, test(zoneSchedule, cal.getTime()));
    }

    public void testLadder() {
        
        SortedMap<Period, ZoneStatus> zoneSchedule = new TreeMap<Period, ZoneStatus>();
        
        Period p1 = new Period("period 1", "00:10", "00:30", ".......");
        Period p2 = new Period("period 2", "00:20", "00:40", ".......");
        Period p3 = new Period("period 3", "00:30", "00:50", ".......");

        zoneSchedule.put(p1, null);
        zoneSchedule.put(p2, null);
        zoneSchedule.put(p3, null);

        
        Calendar cal = new GregorianCalendar();
        
        cal.set(2010, 0, 19, 0, 15);
        assertEquals("Improper match", p1, test(zoneSchedule, cal.getTime()));

        cal.set(2010, 0, 19, 0, 25);
        assertEquals("Improper match", p2, test(zoneSchedule, cal.getTime()));

        cal.set(2010, 0, 19, 0, 35);
        assertEquals("Improper match", p3, test(zoneSchedule, cal.getTime()));

        cal.set(2010, 0, 19, 0, 45);
        assertEquals("Improper match", p3, test(zoneSchedule, cal.getTime()));

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

        
        Calendar cal = new GregorianCalendar();
        
        cal.set(2010, 0, 19, 0, 12);
        assertEquals("Improper match", p1, test(zoneSchedule, cal.getTime()));

        cal.set(2010, 0, 19, 0, 18);
        assertEquals("Improper match", p2, test(zoneSchedule, cal.getTime()));

        cal.set(2010, 0, 19, 0, 22);
        assertEquals("Improper match", p3, test(zoneSchedule, cal.getTime()));

        cal.set(2010, 0, 19, 0, 32);
        assertEquals("Improper match", p2, test(zoneSchedule, cal.getTime()));

        cal.set(2010, 0, 19, 0, 42);
        assertEquals("Improper match", p1, test(zoneSchedule, cal.getTime()));
    }

    public Period test(SortedMap<Period, ZoneStatus> zoneSchedule, Date time) {
        
        return new PeriodMatcher().match(zoneSchedule, time.getTime());
    }
}
