package net.sf.dz3.scheduler.gcal;

import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import net.sf.dz3.device.model.Thermostat;
import net.sf.dz3.device.model.ThermostatSignal;
import net.sf.dz3.device.model.ZoneStatus;
import net.sf.dz3.instrumentation.Marker;
import net.sf.dz3.scheduler.Period;
import net.sf.dz3.scheduler.PeriodMatcher;
import net.sf.dz3.scheduler.ScheduleUpdater;
import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.datastream.signal.model.DataSink;
import net.sf.jukebox.jmx.JmxDescriptor;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import com.google.api.client.util.DateTime;

/**
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2014
 */
public class GCalScheduleUpdaterTest extends TestCase {

    private final Logger logger = Logger.getLogger(getClass());

    public void testAuth() throws IOException {

        // Nothing to test here as ov API v3 - testYesterday() will walk down this path anyway.
    }
    
    public void testTzShift() {
        
        DateTime dt = DateTime.parseRfc3339("2010-01-30T18:00:00.000-07:00");
        
        logger.info("GMT-7: " + dt);
        logger.info("TZ Shift: " + dt.getTimeZoneShift());
        
        // This operation doesn't change the time, but changes the representation
        
        dt = new DateTime(new Date(dt.getValue()), TimeZone.getTimeZone("GMT"));

        logger.info("GMT: " + dt);
        
        assertEquals("TZ shift problem", "2010-01-31T01:00:00.000Z", dt.toString());
    }

    /**
     * Test case to reproduce and fix {@link https://github.com/home-climate-control/dz/issues/6}.
     */
    public void testDST() {
        
        // VT: NOTE: This test will try to spawn the system browser, or pring a link you need to visit to get the callback.
        // You will need to visit the link on the same box, the callback URL is pointing to 'localhost'.
        // Make sure you have a browser that can deal with the page (Lynx, as of 2.8.8dev.12, can't deal with the buttons on the callback page.
        
        // In the worst case, just @Ignore this test. 
        
        NDC.push("testDST");
        Marker m = new Marker("testDST");
        
        try {
        
            Map<Thermostat, String> ts2source = new TreeMap<Thermostat, String>();
            
            // This calendar has to be in Mountain Time Zone with the DST offset present, so do all the events (check individually
            // if your time zone is different).
            // It has to contain 24 periods, spanning from $hour to ($hour + 1) (except the last for now, which has to end at 23:59).
            // Events have to be recurring, with recurrence set to "daily, never expires" (careful: it'll expire in 2 years anyway). 

            Thermostat ts1 = new NullThermostat("DZ Test Case: DST Test (with DST)");

            ts2source.put(ts1, ts1.getName());

            logger.info("Targets: " + ts2source);

            ScheduleUpdater updater = new GCalScheduleUpdater(ts2source);

            long start = System.currentTimeMillis();

            Map<Thermostat, SortedMap<Period, ZoneStatus>> schedule = updater.update();

            logger.info("Took " + (System.currentTimeMillis() - start) + "ms to complete");
            logger.info("Schedule retrieved: " + schedule);

            SortedMap<Period, ZoneStatus> events = schedule.values().iterator().next();
            
            testEvents(events);
            
        } catch (AssertionFailedError ex) {
            
            throw ex;
            
        } catch (Throwable t) {
            
            logger.error("Oops", t);
            fail("Unexpected exception, see logs");
            
        } finally {
            
            m.close();
            NDC.pop();
        }
    }
    
    private void testEvents(SortedMap<Period,ZoneStatus> events) {
        
        Map<Integer, String> offset2name = new TreeMap<Integer, String>();
        
        // The key is the hour offset, the value is the period name in the calendar

        offset2name.put(0, "0 - 1");
        offset2name.put(1, "1 - 2");
        offset2name.put(2, "2 - 3");
        offset2name.put(3, "3 - 4");
        offset2name.put(4, "4 - 5");
        offset2name.put(5, "5 - 6");
        offset2name.put(6, "6 - 7");
        offset2name.put(7, "7 - 8");
        offset2name.put(8, "8 - 9");
        offset2name.put(9, "9 - 10");
        offset2name.put(10, "10 - 11");
        offset2name.put(11, "11 - 12");
        offset2name.put(12, "12 - 13");
        offset2name.put(13, "13 - 14");
        offset2name.put(14, "14 - 15");
        offset2name.put(15, "15 - 16");
        offset2name.put(16, "16 - 17");
        offset2name.put(17, "17 - 18");
        offset2name.put(18, "18 - 19");
        offset2name.put(19, "19 - 20");
        offset2name.put(20, "20 - 21");
        offset2name.put(21, "21 - 22");
        offset2name.put(22, "22 - 23");
        offset2name.put(23, "23 - 24");
        
        PeriodMatcher pm = new PeriodMatcher();
        
        // DST
        TimeZone tzMST = TimeZone.getTimeZone("America/Denver");
        // No DST
        TimeZone tzPHX = TimeZone.getTimeZone("America/Phoenix");
        
        logger.info("TZ/MST: " + tzMST);
        logger.info("TZ/PHX: " + tzPHX);
        
        Calendar calMST = Calendar.getInstance(tzMST);
        Calendar calPHX = Calendar.getInstance(tzPHX);
        
        // DST is guaranteed to be active on this day
        
        calMST.set(2015, Calendar.JULY, 1, 0, 0, 0);
        calPHX.set(2015, Calendar.JULY, 1, 0, 0, 0);
        
        calMST.set(Calendar.MILLISECOND, 0);
        calPHX.set(Calendar.MILLISECOND, 0);

        // java.util.Date doesn't do this right, but DateTime does

        DateTime dtMST = new DateTime(calMST.getTime(), tzMST);
        DateTime dtPHX = new DateTime(calPHX.getTime(), tzPHX);
        
        logger.info("time/DST: " + dtMST);
        logger.info("time/PHX: " + dtPHX);

        calMST.set(Calendar.MINUTE, 30);
        calPHX.set(Calendar.MINUTE, 30);
        
        for (int hour = 0; hour < 24; hour++) {

            calMST.set(Calendar.HOUR_OF_DAY, hour);
            calPHX.set(Calendar.HOUR_OF_DAY, hour);

            logger.info("Matching DST time: " + new DateTime(calMST.getTime(), tzMST));
            logger.info("Matching PHX time: " + new DateTime(calPHX.getTime(), tzPHX));
            
            long timeMST = calMST.getTimeInMillis();
            
            Period p = pm.match(events, timeMST);

            logger.info("Period matched: " + p);
            
            assertEquals("Wrong period matched", offset2name.get(hour), p.name);
        }
    }
    
    /**
     * Test case for http://code.google.com/p/diy-zoning/issues/detail?id=14
     */
    public void testBigOffset() {
        
        NDC.push("testBigOffset");
        
        try {
        
        Calendar tempCalendar = new GregorianCalendar();
        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        NumberFormat tzFormatter = new DecimalFormat("+#00;-#00");
        
        tempCalendar.set(Calendar.HOUR_OF_DAY, 0);
        tempCalendar.set(Calendar.MINUTE, 0);
        tempCalendar.set(Calendar.SECOND, 0);
        tempCalendar.set(Calendar.MILLISECOND, 0);
        
//        tempCalendar.setTimeZone(TimeZone.getTimeZone("GMT+1200"));
        
        Date start = tempCalendar.getTime();

        tempCalendar.set(Calendar.HOUR_OF_DAY, 23);
        tempCalendar.set(Calendar.MINUTE, 59);
        tempCalendar.set(Calendar.SECOND, 59);
        tempCalendar.set(Calendar.MILLISECOND, 0);
        
        Date end = tempCalendar.getTime();
        
        int tzShift = tempCalendar.getTimeZone().getRawOffset() / (60000 * 60);
        String tzTail = tzFormatter.format(tzShift) + ":00";
        
        logger.debug("Start date with no TZ offset: " + dateFormatter.format(start));
        
        String startString = dateFormatter.format(start) + tzTail;
        String endString = dateFormatter.format(end) + tzTail;
        
        logger.debug("String to parse: " + startString);
        
        DateTime.parseRfc3339(startString);
        DateTime.parseRfc3339(endString);
        
        logger.debug("Parsed OK");
        
        } catch (Throwable t) {
            
            logger.error("Oops", t);
            
            fail(t.getMessage());
            
        } finally {
            NDC.pop();
        }
    }

    private static class NullThermostat implements Thermostat {

        private final String name;
        
        public NullThermostat(String name) {
            
            this.name = name;
        }

        @Override
        public String getName() {

            return name;
        }

        @Override
        public double getSetpoint() {
            // Inconsequential for the test case
            return 0;
        }

        @Override
        public ThermostatSignal getSignal() {
            // Inconsequential for the test case
            return null;
        }

        @Override
        public void raise() {
            // Inconsequential for the test case

        }

        @Override
        public void set(ZoneStatus status) {
            // Inconsequential for the test case

        }

        @Override
        public double getControlSignal() {
            // Inconsequential for the test case
            return 0;
        }

        @Override
        public boolean isError() {
            // Inconsequential for the test case
            return false;
        }

        @Override
        public boolean isOnHold() {
            // Inconsequential for the test case
            return false;
        }

        @Override
        public int getDumpPriority() {
            // Inconsequential for the test case
            return 0;
        }

        @Override
        public boolean isOn() {
            // Inconsequential for the test case
            return false;
        }

        @Override
        public boolean isVoting() {
            // Inconsequential for the test case
            return false;
        }

        @Override
        public void consume(DataSample<Double> signal) {
            // Inconsequential for the test case

        }

        @Override
        public void addConsumer(DataSink<ThermostatSignal> consumer) {
            // Inconsequential for the test case

        }

        @Override
        public void removeConsumer(DataSink<ThermostatSignal> consumer) {
            // Inconsequential for the test case

        }

        @Override
        public int compareTo(Thermostat o) {
            
            return getName().compareTo(o.getName());
        }

        @Override
        public JmxDescriptor getJmxDescriptor() {
            // Inconsequential for the test case
            return null;
        }
        
        public String toString() {
            
            return getName();
        }
    }
}
