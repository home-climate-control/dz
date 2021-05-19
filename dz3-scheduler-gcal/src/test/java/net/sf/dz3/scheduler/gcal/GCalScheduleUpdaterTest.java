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
import java.util.TreeMap;

import junit.framework.TestCase;
import net.sf.dz3.device.model.Thermostat;
import net.sf.dz3.device.model.ThermostatSignal;
import net.sf.dz3.device.model.ZoneStatus;
import net.sf.dz3.scheduler.Period;
import net.sf.dz3.scheduler.PeriodMatcher;
import net.sf.dz3.scheduler.ScheduleUpdater;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import com.google.gdata.data.DateTime;

/**
 * 
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2012
 */
public class GCalScheduleUpdaterTest extends TestCase {

    private final Logger logger = Logger.getLogger(getClass());

    // Temporarily disabled, testYesterday() will test this anyway
    public void XtestAuth() throws IOException {
        
        Map<Thermostat, String> ts2source = new TreeMap<Thermostat, String>();
        
        Thermostat ts1 = new NullThermostat("DZ Schedule: Single Zone Sample");
        Thermostat ts2 = new NullThermostat("Office");
        Thermostat ts3 = new NullThermostat("This zone does not exist");
        
        ts2source.put(ts1, ts1.getName());
        ts2source.put(ts2, ts2.getName());
        ts2source.put(ts3, ts3.getName());
        
        logger.info("Targets: " + ts2source);
        
        String username = System.getProperty("GCAL_SHEDULE_UPDATER_USERNAME");
        String password = System.getProperty("GCAL_SHEDULE_UPDATER_PASSWORD");
        String domain = System.getProperty("GCAL_SHEDULE_UPDATER_DOMAIN");
        
        if (username == null || "".equals(username)) {
            
            logger.error("Username not set, test not run",
                    new Exception("Use this trace to see what you have to do to run the test"));
            
            return;
        }
        
        logger.info("Using " + username + ":" + password + "@" + domain + " for authentication");

        ScheduleUpdater updater = new GCalScheduleUpdater(ts2source, username, password, domain);
        
        long start = System.currentTimeMillis();
        
        Map<Thermostat, SortedMap<Period, ZoneStatus>> schedule = updater.update();
        
        logger.info("Took " + (System.currentTimeMillis() - start) + "ms to complete");
        logger.info("Schedule retrieved: " + schedule);
    }
    
    public void testTzShift() {
        
        DateTime dt = DateTime.parseDateTime("2010-01-30T18:00:00.000-07:00");
        
        logger.info("GMT-7: " + dt);
        logger.info("TZ Shift: " + dt.getTzShift());
        
        // This operation doesn't change the time, but changes the representation
        dt.setTzShift(0);

        logger.info("GMT: " + dt);
        
        assertEquals("TZ shift problem", "2010-01-31T01:00:00.000Z", dt.toString());
    }

    public void testYesterday() {
        
        NDC.push("testYesterday");
        
        try {
        
            Map<Thermostat, String> ts2source = new TreeMap<Thermostat, String>();

//            Thermostat ts1 = new NullThermostat("DZ Test Case: MB");
            Thermostat ts1 = new NullThermostat("DZ Test Case: IO");
//            Thermostat ts1 = new NullThermostat("DZ Test Case: TO");

            ts2source.put(ts1, ts1.getName());

            logger.info("Targets: " + ts2source);

            String username = System.getProperty("GCAL_SHEDULE_UPDATER_USERNAME");
            String password = System.getProperty("GCAL_SHEDULE_UPDATER_PASSWORD");
            String domain = System.getProperty("GCAL_SHEDULE_UPDATER_DOMAIN");

            if (username == null || "".equals(username)) {

                logger.error("Username not set, test not run",
                        new Exception("Use this trace to see what you have to do to run the test"));

                return;
            }

            logger.info("Using " + username + ":" + password + "@" + domain + " for authentication");

            ScheduleUpdater updater = new GCalScheduleUpdater(ts2source, username, password, domain);

            long start = System.currentTimeMillis();

            Map<Thermostat, SortedMap<Period, ZoneStatus>> schedule = updater.update();

            logger.info("Took " + (System.currentTimeMillis() - start) + "ms to complete");
            logger.info("Schedule retrieved: " + schedule);

            SortedMap<Period, ZoneStatus> events = schedule.values().iterator().next();
            PeriodMatcher pm = new PeriodMatcher();

            Period p = pm.match(events, System.currentTimeMillis());

            logger.info("Period matched: " + p);

        } catch (Throwable t) {
            
            logger.error("Oops", t);
            fail("Unexpected exception, see logs");
            
        } finally {
            NDC.pop();
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
        
        @SuppressWarnings("unused")
        DateTime dtStart = DateTime.parseDateTime(startString);
        @SuppressWarnings("unused")
        DateTime dtEnd = DateTime.parseDateTime(endString);
        
        logger.debug("Parsed OK");
        
        } catch (Throwable t) {
            
            logger.error("Oops", t);
            
            fail(t.getMessage());
            
        } finally {
            NDC.pop();
        }
    }

    private class NullThermostat implements Thermostat {

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
