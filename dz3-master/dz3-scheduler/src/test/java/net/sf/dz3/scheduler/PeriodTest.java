package net.sf.dz3.scheduler;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.SortedSet;
import java.util.TreeSet;

import junit.framework.TestCase;

public class PeriodTest extends TestCase {
    
    private final long TWO_FIFTEEN = 2 * 1000 * 60 * 60 + 15 * 1000 * 60;
    private final long FOURTEEN_FIFTEEN = 14 * 1000 * 60 * 60 + 15 * 1000 * 60;
    
    public void testTwoFifteen() {
        
        Period p = new Period("period", "02:15", "02:20", "       ");
        
        assertEquals("Wrong time", TWO_FIFTEEN, p.start);
        assertEquals("Wrong days", 0x00, p.days);
        assertEquals("Wrong string representation", "period (02:15 to 02:20 on .......)", p.toString());
    }

    public void testFourteenFifteen() {
        
        Period p = new Period("period", "14:15", "14:20", "       ");
        
        assertEquals("Wrong time", FOURTEEN_FIFTEEN, p.start);
        assertEquals("Wrong days", 0x00, p.days);
        assertEquals("Wrong string representation", "period (14:15 to 14:20 on .......)", p.toString());
    }

    public void testDaysTooShort() {
        
        try {
            
            Period p = new Period("period", "0:15", "0:20", "      ");

            assertEquals("Wrong time", 15 * 1000 * 60, p.start);
            assertEquals("Wrong days", 0x00, p.days);
            
            fail("Should have thrown an exception by now");
            
        } catch (IllegalArgumentException ex) {
            
            assertEquals("Wrong exception message", "days argument malformed, see source code for instructions", ex.getMessage());
        }
    }

    public void testDaysTooLong() {
        
        try {
            
            Period p = new Period("period", "0:15", "0:20", "        ");

            assertEquals("Wrong time", 15 * 1000 * 60, p.start);
            assertEquals("Wrong days", 0x00, p.days);
            
            fail("Should have thrown an exception by now");
            
        } catch (IllegalArgumentException ex) {
            
            assertEquals("Wrong exception message", "days argument malformed, see source code for instructions", ex.getMessage());
        }
    }

    public void testDaysMWTS() {
        
        Period p = new Period("period", "0:15", "0:20", "M WT  S");

        assertEquals("Wrong time", 15 * 1000 * 60, p.start);
        assertEquals("Wrong days", 0x4D, p.days);
    }

    public void testDaysMTSS() {
        
        Period p = new Period("period", "0:15","0:20",  "MT   SS");

        assertEquals("Wrong time", 15 * 1000 * 60, p.start);
        assertEquals("Wrong days", 0x63, p.days);
    }

    public void testDaysMTWTFSS() {
        
        Period p = new Period("period", "0:15","0:20",  ".......");

        assertEquals("Wrong time", 15 * 1000 * 60, p.start);
        assertEquals("Wrong days", 0x7F, p.days);
    }

    public void testTimeAM() {
        
        Period p = new Period("period", "2:15 AM","02:20 AM",  ".......");

        assertEquals("Wrong time", TWO_FIFTEEN, p.start);
        assertEquals("Wrong days", 0x7F, p.days);
    }

    public void testTimePM() {
        
        Period p = new Period("period", "2:15 PM", "02:20 PM", ".......");
        
        assertEquals("Wrong time", FOURTEEN_FIFTEEN, p.start);
        assertEquals("Wrong days", 0x7F, p.days);
    }

    public void testTimeMilitary() {
        
        Period p = new Period("period", "1415", "1420", ".......");

        assertEquals("Wrong time", FOURTEEN_FIFTEEN, p.start);
        assertEquals("Wrong days", 0x7F, p.days);
    }

    public void testBadTime() {
        
        try {
            
            Period p = new Period("period", "oops", "oops again", "       ");

            assertEquals("Wrong time", 15 * 1000 * 60, p.start);
            assertEquals("Wrong days", 0x00, p.days);
            
            fail("Should have thrown an exception by now");
            
        } catch (IllegalArgumentException ex) {
        	
            assertEquals("Wrong exception message",
                    "Tried all available formats ('yy-MM-dd'T'hh:mm', 'KK:mm aa', 'hh:mm aa', 'HH:mm', 'HHmm') to parse 'oops'and failed, giving up",
                    ex.getMessage());
        }
    }
    
    public void testCompareTo() {
        
        Period p1 = new Period("period 1", "1415", "1420", ".......");
        Period p2 = new Period("period 2", "1416", "1421", ".......");
        int result = p1.compareTo(p2);
        
        assertEquals("Wrong comparison result", -1000 * 60, result);
        
        SortedSet<Period> set = new TreeSet<Period>();
        
        set.add(p2);
        set.add(p1);
        
        assertEquals("Wrong element retrieved", p1, set.first());
    }
    
    public void testCompareToSameStart() {
        
        Period p1 = new Period("period 1", "1415", "1420", ".......");
        Period p2 = new Period("period 2", "1415", "1425", ".......");
        int result = p1.compareTo(p2);
        
        assertEquals("Wrong comparison result", 300000, result);
        
        SortedSet<Period> set = new TreeSet<Period>();
        
        set.add(p2);
        set.add(p1);
        
        assertEquals("Wrong element retrieved", p2, set.first());
        assertEquals("Wrong item count", set.size(), 2);
    }

    public void testDayOffset() {
        
        assertEquals("MO", 0, sunday2monday(1));
        assertEquals("TU", 1, sunday2monday(2));
        assertEquals("WE", 2, sunday2monday(3));
        assertEquals("TH", 3, sunday2monday(4));
        assertEquals("FR", 4, sunday2monday(5));
        assertEquals("SA", 5, sunday2monday(6));
        assertEquals("SU", 6, sunday2monday(0));
        
    }
    
    private int sunday2monday(int sunday) {
        
        return (sunday + 6) % 7;
    }
    
    public void testIncludesDayMo() {
        
        Calendar cal = new GregorianCalendar();
        
        cal.set(2010, 0, 18);
        testIncludesDay(cal.getTime(), "M      ");
        
        cal.set(2010, 0, 19);
        testIncludesDay(cal.getTime(), " T     ");
        
        cal.set(2010, 0, 20);
        testIncludesDay(cal.getTime(), "  W    ");
        
        cal.set(2010, 0, 21);
        testIncludesDay(cal.getTime(), "   T   ");
        
        cal.set(2010, 0, 22);
        testIncludesDay(cal.getTime(), "    F  ");
        
        cal.set(2010, 0, 23);
        testIncludesDay(cal.getTime(), "     S ");
        
        cal.set(2010, 0, 24);
        testIncludesDay(cal.getTime(), "      S");
        
    }
    
    private void testIncludesDay(Date d, String days) {
        
        Period p = new Period("period", "1415", "1420", days);
        
        assertTrue("Wrong inclusion for " + d, p.includesDay(d));
    }
    
    public void testToString() {
        
        Period p = new Period("period", "1415", "1420", ".......");
        
        assertEquals("Wrong string representation", "period (14:15 to 14:20 on MTWTFSS)", p.toString());
    }
}
