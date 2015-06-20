package net.sf.dz3.scheduler;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import net.sf.dz3.device.model.Thermostat;
import net.sf.dz3.device.model.ThermostatSignal;
import net.sf.dz3.device.model.ZoneStatus;
import net.sf.dz3.device.model.impl.ZoneStatusImpl;
import net.sf.dz3.scheduler.Scheduler.Deviation;
import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.datastream.signal.model.DataSink;
import net.sf.jukebox.jmx.JmxDescriptor;
import junit.framework.TestCase;

public class SchedulerTest extends TestCase {
    
    private final Logger logger = Logger.getLogger(getClass());
    private final Random rg = new Random();
    
    public void testDeviation() {
        
        double setpoint = rg.nextDouble();
        boolean enabled = rg.nextBoolean();
        boolean voting = rg.nextBoolean();
        
        Deviation d = new Deviation(setpoint, enabled, voting);
        
        assertEquals(setpoint,d.setpoint);
        assertEquals(enabled, d.enabled);
        assertEquals(voting, d.voting);
    }

    public void testInstantiation() {
        
        final Map<Thermostat, SortedMap<Period, ZoneStatus>> schedule = new TreeMap<Thermostat, SortedMap<Period, ZoneStatus>>();
        ScheduleUpdater updater = new ScheduleUpdater() {

            @Override
            public Map<Thermostat, SortedMap<Period, ZoneStatus>> update() throws IOException {
                return schedule;
            }
        };
        
        new Scheduler();
        new Scheduler(schedule);
        new Scheduler(updater);
        new Scheduler(updater, null);
    }
    
    public void testGranularity() {
        
        Scheduler s = new Scheduler();
        
        // This should be fine
        s.setScheduleGranularity(1);
        
        try {

            // but this is not
            s.setScheduleGranularity(0);

        } catch (IllegalArgumentException ex) {
            assertEquals("Wrong exception message", "0: value doesn't make sense", ex.getMessage());
        }

        long value = -1 * Math.abs(rg.nextLong());

        try {
            // and neither is this
            s.setScheduleGranularity(value);

        } catch (IllegalArgumentException ex) {
            assertEquals("Wrong exception message", value + ": value doesn't make sense", ex.getMessage());
        }
    }
    
    /**
     * Test the no-argument {@link Scheduler#start()} method.
     */
    public void testStart() {
        
        NDC.push("testStart");
        
        try {
        
            Scheduler s = new Scheduler();
            
            s.start();
            
        } finally {
            NDC.pop();
        }
    }

    public void testFastStart() {
        
        NDC.push("testFastStart");
        
        try {

            ScheduleUpdater u = new ScheduleUpdater() {
                
                @Override
                public Map<Thermostat, SortedMap<Period, ZoneStatus>> update() throws IOException {

                    // Bad implementation, but shouldn't break anything
                    return null;
                }
            };
            
            Scheduler s = new Scheduler(u);
            
            s.setScheduleGranularity(50);
            
            // This instance will run until the JVM is gone or Scheduler#ScheduledExecutorService is otherwise stopped 
            s.start(0);
            
            Thread.sleep(100);

        } catch (InterruptedException ex) {

            throw new IllegalStateException(ex);
            
        } finally {
            NDC.pop();
        }
    }

    public void testStartStop() {
        
        NDC.push("testStartStop");
        
        try {
            
            final Semaphore syncLock = new Semaphore(1);

            ScheduleUpdater u = new ScheduleUpdater() {
                
                @Override
                public Map<Thermostat, SortedMap<Period, ZoneStatus>> update() throws IOException {
                    
                    NDC.push("update");
                    
                    try {
                        
                        logger.info("started");
                        
                        syncLock.acquire();
                        
                        logger.info("got the lock");

                        // This timeout should be longer than the run timeout so we can test the stop() properly
                        Thread.sleep(200);
                        
                        logger.info("done");
                        
                        return new TreeMap<Thermostat, SortedMap<Period, ZoneStatus>>();

                    } catch (InterruptedException ex) {
                     
                        logger.info("Interrupted", ex);
                        return null;
                        
                    } finally {
                        NDC.pop();
                    }
                }
            };
            
            Scheduler s = new Scheduler(u);
            
            s.setScheduleGranularity(50);

            // Acquire the lock so update() will wait until it is released
            
            syncLock.acquire();
            s.start(0);
            
            // Wait for a bit so update() has a chance to run
            Thread.sleep(100);
            
            logger.info("releasing the lock");
            syncLock.release();
            
            // Wait for a bit so update() has a chance to acquire the lock and start waiting
            Thread.sleep(50);

            s.stop();

        } catch (InterruptedException ex) {

            throw new IllegalStateException(ex);
            
        } finally {
            NDC.pop();
        }
    }

    public void testIOException() {
        
        NDC.push("testIOException");
        
        try {

            ScheduleUpdater u = new ScheduleUpdater() {
                
                @Override
                public Map<Thermostat, SortedMap<Period, ZoneStatus>> update() throws IOException {

                    throw new IOException("Ouch!");
                }
            };
            
            Scheduler s = new Scheduler(u);
            
            s.setScheduleGranularity(50);
            
            // This instance will run until the JVM is gone or Scheduler#ScheduledExecutorService is otherwise stopped 
            s.start(0);
            
            Thread.sleep(50);
            
            s.stop();

        } catch (InterruptedException ex) {

            throw new IllegalStateException(ex);
            
        } finally {
            NDC.pop();
        }
    }

    public void testExecute() {
        
        List<Map<Thermostat, SortedMap<Period, ZoneStatus>>> schedules = new LinkedList<Map<Thermostat, SortedMap<Period, ZoneStatus>>>();
        
        schedules.add(renderSchedule1());
        schedules.add(renderSchedule2());
        schedules.add(renderSchedule3());
        
        int id = 0;
        for (Iterator<Map<Thermostat, SortedMap<Period, ZoneStatus>>> i = schedules.iterator(); i.hasNext(); ) {
            
            testExecute(id++, i.next());
        }
    }
    
    public void testExecute(int id, final Map<Thermostat, SortedMap<Period, ZoneStatus>> schedule) {
        
        NDC.push("testExecute:" + id);
        
        try {
            
            ScheduleUpdater u = new ScheduleUpdater() {
                
                @Override
                public Map<Thermostat, SortedMap<Period, ZoneStatus>> update() throws IOException {

                    return schedule;
                }
            };
            
            Scheduler s = new Scheduler(u);
            
            s.setScheduleGranularity(50);
            
            // This instance will run until the JVM is gone or Scheduler#ScheduledExecutorService is otherwise stopped 
            s.start(0);
            
            Thread.sleep(50);
            
            s.stop();

        } catch (InterruptedException ex) {

            throw new IllegalStateException(ex);
            
        } finally {
            NDC.pop();
        }
    }
    
    /**
     * Render the schedule with overlapping periods as follows:
     * 
     *  Night: midnight to 9:00
     *  Morning: 8:00 to 10:00
     *  Day: 9:00 to 21:00
     *  Evening: 18:00 to 23:59
     */
    private Map<Thermostat, SortedMap<Period, ZoneStatus>> renderSchedule1() {
        
        final Map<Thermostat, SortedMap<Period, ZoneStatus>> result = new TreeMap<Thermostat, SortedMap<Period, ZoneStatus>>();
        Thermostat t = new NullThermostat("thermostat");

        Period pNight = new Period("night", "0:00", "9:00", ".......");
        ZoneStatus zsNight = new ZoneStatusImpl(25.0, 0, true, true);

        Period pMorning = new Period("morning", "8:00", "10:00", ".......");
        ZoneStatus zsMorning = new ZoneStatusImpl(24.5, 0, true, true);

        Period pDay = new Period("day", "9:00", "21:00", ".......");
        ZoneStatus zsDay = new ZoneStatusImpl(30, 0, true, true);

        Period pEvening = new Period("evening", "18:00", "23:59", ".......");
        ZoneStatus zsEvening = new ZoneStatusImpl(24.8, 0, true, true);
        
        SortedMap<Period, ZoneStatus> periods = new TreeMap<Period, ZoneStatus>();
        
        periods.put(pNight, zsNight);
        periods.put(pMorning, zsMorning);
        periods.put(pDay, zsDay);
        periods.put(pEvening, zsEvening);
        
        result.put(t, periods);
        
        return result;
    }
    
    /**
     * Render the schedule with overlapping periods as follows:
     * 
     *  Background: midnight to 23:59
     *  Morning: 8:00 to 10:00
     *  Day: 9:00 to 14:00
     *  Evening: 18:00 to 23:59
     */
    private Map<Thermostat, SortedMap<Period, ZoneStatus>> renderSchedule2() {
        
        final Map<Thermostat, SortedMap<Period, ZoneStatus>> result = new TreeMap<Thermostat, SortedMap<Period, ZoneStatus>>();
        Thermostat t = new NullThermostat("thermostat");

        Period pBackground = new Period("background", "0:00", "23:59", ".......");
        ZoneStatus zsBackground = new ZoneStatusImpl(25.0, 0, true, true);

        Period pMorning = new Period("morning", "8:00", "10:00", ".......");
        ZoneStatus zsMorning = new ZoneStatusImpl(24.5, 0, true, true);

        Period pDay = new Period("day", "9:00", "14:00", ".......");
        ZoneStatus zsDay = new ZoneStatusImpl(30, 0, true, true);

        Period pEvening = new Period("evening", "18:00", "23:59", ".......");
        ZoneStatus zsEvening = new ZoneStatusImpl(24.8, 0, true, true);
        
        SortedMap<Period, ZoneStatus> periods = new TreeMap<Period, ZoneStatus>();
        
        periods.put(pBackground, zsBackground);
        periods.put(pMorning, zsMorning);
        periods.put(pDay, zsDay);
        periods.put(pEvening, zsEvening);
        
        result.put(t, periods);
        
        return result;
    }
    
    /**
     * Render the schedule with gaps as follows:
     * 
     *  Morning: 8:00 to 10:00
     *  Evening: 18:00 to 23:00
     */
    private Map<Thermostat, SortedMap<Period, ZoneStatus>> renderSchedule3() {
        
        final Map<Thermostat, SortedMap<Period, ZoneStatus>> result = new TreeMap<Thermostat, SortedMap<Period, ZoneStatus>>();
        Thermostat t = new NullThermostat("thermostat");

        Period pMorning = new Period("morning", "8:00", "10:00", ".......");
        ZoneStatus zsMorning = new ZoneStatusImpl(24.5, 0, true, true);

        Period pEvening = new Period("evening", "18:00", "22:00", ".......");
        ZoneStatus zsEvening = new ZoneStatusImpl(24.8, 0, true, true);
        
        SortedMap<Period, ZoneStatus> periods = new TreeMap<Period, ZoneStatus>();
        
        periods.put(pMorning, zsMorning);
        periods.put(pEvening, zsEvening);
        
        result.put(t, periods);
        
        return result;
    }
    
    /**
     * @return Time for 00:00 of current week's Monday.
     */
    private Calendar getMondayStart() {
        
        NDC.push("getMondayStart");
        
        try {
        
            Calendar cal = new GregorianCalendar();
            
            logger.debug("calendar: " + cal);
            
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            
            logger.debug("calendar: " + cal);

            return cal;
        
        } finally {
            NDC.pop();
        }
    }
    
    public void testSchedule1() {
        
        NDC.push("testSchedule1");
        
        try {
        
            checkSchedule1(renderSchedule1(), new Scheduler());
            
        } finally {
            NDC.pop();
        }
    }

    private void checkSchedule1(Map<Thermostat, SortedMap<Period, ZoneStatus>> schedule, Scheduler s) {

        Entry<Thermostat, SortedMap<Period, ZoneStatus>> entry = schedule.entrySet().iterator().next();
        Thermostat ts = entry.getKey();
                
        Calendar cal = getMondayStart();

        s.execute(schedule, cal.getTimeInMillis());
        
        assertEquals("Wrong period for " + cal.getTime(), "night", s.getCurrentPeriod(ts).name);
        
        cal.set(Calendar.HOUR_OF_DAY, 8);
        cal.set(Calendar.MINUTE, 30);
        
        s.execute(schedule, cal.getTimeInMillis());
        
        assertEquals("Wrong period for " + cal.getTime(), "morning", s.getCurrentPeriod(ts).name);
        
        cal.set(Calendar.HOUR_OF_DAY, 9);
        cal.set(Calendar.MINUTE, 0);
        
        s.execute(schedule, cal.getTimeInMillis());
        
        assertEquals("Wrong period for " + cal.getTime(), "day", s.getCurrentPeriod(ts).name);
        
        cal.set(Calendar.HOUR_OF_DAY, 10);
        cal.set(Calendar.MINUTE, 0);
        
        s.execute(schedule, cal.getTimeInMillis());
        
        assertEquals("Wrong period for " + cal.getTime(), "day", s.getCurrentPeriod(ts).name);
        
        cal.set(Calendar.HOUR_OF_DAY, 18);
        cal.set(Calendar.MINUTE, 0);
        
        s.execute(schedule, cal.getTimeInMillis());
        
        assertEquals("Wrong period for " + cal.getTime(), "evening", s.getCurrentPeriod(ts).name);
        
        cal.set(Calendar.HOUR_OF_DAY, 21);
        cal.set(Calendar.MINUTE, 0);
        
        s.execute(schedule, cal.getTimeInMillis());
        
        assertEquals("Wrong period for " + cal.getTime(), "evening", s.getCurrentPeriod(ts).name);
    }

    public void testSchedule2() {
        
        NDC.push("testSchedule2");
        
        try {
        
            checkSchedule2(renderSchedule2(), new Scheduler());
            
        } finally {
            NDC.pop();
        }
    }

    private void checkSchedule2(Map<Thermostat, SortedMap<Period, ZoneStatus>> schedule, Scheduler s) {

        Entry<Thermostat, SortedMap<Period, ZoneStatus>> entry = schedule.entrySet().iterator().next();
        Thermostat ts = entry.getKey();
                
        Calendar cal = getMondayStart();

        s.execute(schedule, cal.getTimeInMillis());
        
        assertEquals("Wrong period for " + cal.getTime(), "background", s.getCurrentPeriod(ts).name);
        
        cal.set(Calendar.HOUR_OF_DAY, 8);
        cal.set(Calendar.MINUTE, 30);
        
        s.execute(schedule, cal.getTimeInMillis());
        
        assertEquals("Wrong period for " + cal.getTime(), "morning", s.getCurrentPeriod(ts).name);
        
        cal.set(Calendar.HOUR_OF_DAY, 9);
        cal.set(Calendar.MINUTE, 0);
        
        s.execute(schedule, cal.getTimeInMillis());
        
        assertEquals("Wrong period for " + cal.getTime(), "day", s.getCurrentPeriod(ts).name);
        
        cal.set(Calendar.HOUR_OF_DAY, 10);
        cal.set(Calendar.MINUTE, 0);
        
        s.execute(schedule, cal.getTimeInMillis());
        
        assertEquals("Wrong period for " + cal.getTime(), "day", s.getCurrentPeriod(ts).name);
        
        cal.set(Calendar.HOUR_OF_DAY, 15);
        cal.set(Calendar.MINUTE, 0);
        
        s.execute(schedule, cal.getTimeInMillis());
        
        assertEquals("Wrong period for " + cal.getTime(), "background", s.getCurrentPeriod(ts).name);
        
        cal.set(Calendar.HOUR_OF_DAY, 18);
        cal.set(Calendar.MINUTE, 0);
        
        s.execute(schedule, cal.getTimeInMillis());
        
        assertEquals("Wrong period for " + cal.getTime(), "evening", s.getCurrentPeriod(ts).name);
        
        cal.set(Calendar.HOUR_OF_DAY, 21);
        cal.set(Calendar.MINUTE, 0);
        
        s.execute(schedule, cal.getTimeInMillis());
        
        assertEquals("Wrong period for " + cal.getTime(), "evening", s.getCurrentPeriod(ts).name);
    }

    public void testSchedule3() {
        
        NDC.push("testSchedule3");
        
        try {
        
            checkSchedule3(renderSchedule3(), new Scheduler());
            
        } finally {
            NDC.pop();
        }
    }

    private void checkSchedule3(Map<Thermostat, SortedMap<Period, ZoneStatus>> schedule, Scheduler s) {

        Entry<Thermostat, SortedMap<Period, ZoneStatus>> entry = schedule.entrySet().iterator().next();
        Thermostat ts = entry.getKey();
                
        Calendar cal = getMondayStart();

        s.execute(schedule, cal.getTimeInMillis());
        
        assertEquals("Wrong period for " + cal.getTime(), null, s.getCurrentPeriod(ts));
        
        cal.set(Calendar.HOUR_OF_DAY, 9);
        cal.set(Calendar.MINUTE, 0);
        
        s.execute(schedule, cal.getTimeInMillis());
        
        assertEquals("Wrong period for " + cal.getTime(), "morning", s.getCurrentPeriod(ts).name);
        
        cal.set(Calendar.HOUR_OF_DAY, 15);
        cal.set(Calendar.MINUTE, 0);
        
        s.execute(schedule, cal.getTimeInMillis());
        
        assertEquals("Wrong period for " + cal.getTime(), null, s.getCurrentPeriod(ts));
        
        cal.set(Calendar.HOUR_OF_DAY, 18);
        cal.set(Calendar.MINUTE, 0);
        
        s.execute(schedule, cal.getTimeInMillis());
        
        assertEquals("Wrong period for " + cal.getTime(), "evening", s.getCurrentPeriod(ts).name);
        
        cal.set(Calendar.HOUR_OF_DAY, 21);
        cal.set(Calendar.MINUTE, 0);
        
        s.execute(schedule, cal.getTimeInMillis());
        
        assertEquals("Wrong period for " + cal.getTime(), "evening", s.getCurrentPeriod(ts).name);

        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 0);
        
        s.execute(schedule, cal.getTimeInMillis());
        
        assertEquals("Wrong period for " + cal.getTime(), null, s.getCurrentPeriod(ts));
        
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
