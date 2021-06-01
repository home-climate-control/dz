package net.sf.dz3.scheduler;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;
import net.sf.dz3.device.model.Thermostat;
import net.sf.dz3.device.model.ThermostatSignal;
import net.sf.dz3.device.model.ZoneStatus;
import net.sf.dz3.device.model.impl.ZoneStatusImpl;
import net.sf.dz3.scheduler.Scheduler.Deviation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

class SchedulerTest {
    
    private final Logger logger = LogManager.getLogger(getClass());
    private final Random rg = new Random();

    @Test
    public void testDeviationInstantiation() {
        
        double setpoint = rg.nextDouble();
        boolean enabled = rg.nextBoolean();
        boolean voting = rg.nextBoolean();
        
        Deviation d = new Deviation(setpoint, enabled, voting);
        
        assertThat(d.setpoint).isEqualTo(setpoint);
        assertThat(d.enabled).isEqualTo(enabled);
        assertThat(d.voting).isEqualTo(voting);
    }

    @Test
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

    @Test
    public void testGranularity() {
        
        Scheduler s = new Scheduler();
        
        // This should be fine
        s.setScheduleGranularity(1);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> {
                    // but this is not
                    s.setScheduleGranularity(0);
                })
                .withMessage("0: value doesn't make sense");

        long value = -1 * Math.abs(rg.nextInt(Integer.MAX_VALUE));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> {
                    // and neither is this
                    s.setScheduleGranularity(value);
                })
                .withMessage(value + ": value doesn't make sense");
    }
    
    /**
     * Test the no-argument {@link Scheduler#start()} method.
     */
    @Test
    public void testStart() {
        
        ThreadContext.push("testStart");
        
        try {
        
            Scheduler s = new Scheduler();
            
            s.start();
            
        } finally {
            ThreadContext.pop();
        }
    }

    @Test
    public void testFastStart() {
        
        ThreadContext.push("testFastStart");
        
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
            ThreadContext.pop();
        }
    }

    @Test
    public void testStartStop() {
        
        ThreadContext.push("testStartStop");
        
        try {
            
            final Semaphore syncLock = new Semaphore(1);

            ScheduleUpdater u = new ScheduleUpdater() {
                
                @Override
                public Map<Thermostat, SortedMap<Period, ZoneStatus>> update() throws IOException {
                    
                    ThreadContext.push("update");
                    
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
                        ThreadContext.pop();
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
            ThreadContext.pop();
        }
    }

    @Test
    public void testIOException() {
        
        ThreadContext.push("testIOException");
        
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
            ThreadContext.pop();
        }
    }

    @Test
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
        
        ThreadContext.push("testExecute:" + id);
        
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
            ThreadContext.pop();
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
     * Render the schedule as follows:
     * 
     *  Away: setpoint 29C, all day
     *  Reset: setpoint 29C, 01:00 to 01:30
     *
     *  @see #renderSchedule5()
     */
    private Map<Thermostat, SortedMap<Period, ZoneStatus>> renderSchedule4() {
        
        final Map<Thermostat, SortedMap<Period, ZoneStatus>> result = new TreeMap<Thermostat, SortedMap<Period, ZoneStatus>>();
        Thermostat t = new NullThermostat("thermostat");

        Period pMorning = new Period("away", "0:00", "23:59", ".......");
        ZoneStatus zsAway = new ZoneStatusImpl(29.0, 0, true, true);

        Period pEvening = new Period("reset", "01:00", "01:30", ".......");
        ZoneStatus zsReset = new ZoneStatusImpl(29.0, 0, true, true);
        
        SortedMap<Period, ZoneStatus> periods = new TreeMap<Period, ZoneStatus>();
        
        periods.put(pMorning, zsAway);
        periods.put(pEvening, zsReset);
        
        result.put(t, periods);
        
        return result;
    }

    /**
     * Render the schedule as follows:
     * 
     *  Away: setpoint 29C, all day
     *  Reset: setpoint 29.5C, 01:00 to 01:30
     *
     *  @see #renderSchedule4()
     */
    private Map<Thermostat, SortedMap<Period, ZoneStatus>> renderSchedule5() {
        
        final Map<Thermostat, SortedMap<Period, ZoneStatus>> result = new TreeMap<Thermostat, SortedMap<Period, ZoneStatus>>();
        Thermostat t = new NullThermostat("thermostat");

        Period pMorning = new Period("away", "0:00", "23:59", ".......");
        ZoneStatus zsAway = new ZoneStatusImpl(29.0, 0, true, true);

        Period pEvening = new Period("reset", "01:00", "01:30", ".......");
        ZoneStatus zsReset = new ZoneStatusImpl(29.5, 0, true, true);
        
        SortedMap<Period, ZoneStatus> periods = new TreeMap<Period, ZoneStatus>();
        
        periods.put(pMorning, zsAway);
        periods.put(pEvening, zsReset);
        
        result.put(t, periods);
        
        return result;
    }

    /**
     * @return Time for 00:00 of current week's Monday.
     */
    private Calendar getMondayStart() {
        
        ThreadContext.push("getMondayStart");
        
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
            ThreadContext.pop();
        }
    }

    @Test
    public void testSchedule1() {
        
        ThreadContext.push("testSchedule1");
        
        try {
        
            checkSchedule1(renderSchedule1(), new Scheduler());
            
        } finally {
            ThreadContext.pop();
        }
    }

    private void checkSchedule1(Map<Thermostat, SortedMap<Period, ZoneStatus>> schedule, Scheduler s) {

        Entry<Thermostat, SortedMap<Period, ZoneStatus>> entry = schedule.entrySet().iterator().next();
        Thermostat ts = entry.getKey();
                
        Calendar cal = getMondayStart();

        s.execute(schedule, new DateTime(cal.getTimeInMillis()));

        assertThat(s.getCurrentPeriod(ts).name).as("period for " + cal.getTime()).isEqualTo("night");

        cal.set(Calendar.HOUR_OF_DAY, 8);
        cal.set(Calendar.MINUTE, 30);
        
        s.execute(schedule, new DateTime(cal.getTimeInMillis()));

        assertThat(s.getCurrentPeriod(ts).name).as("period for " + cal.getTime()).isEqualTo("morning");

        cal.set(Calendar.HOUR_OF_DAY, 9);
        cal.set(Calendar.MINUTE, 0);
        
        s.execute(schedule, new DateTime(cal.getTimeInMillis()));

        assertThat(s.getCurrentPeriod(ts).name).as("period for " + cal.getTime()).isEqualTo("day");

        cal.set(Calendar.HOUR_OF_DAY, 10);
        cal.set(Calendar.MINUTE, 0);
        
        s.execute(schedule, new DateTime(cal.getTimeInMillis()));

        assertThat(s.getCurrentPeriod(ts).name).as("period for " + cal.getTime()).isEqualTo("day");

        cal.set(Calendar.HOUR_OF_DAY, 18);
        cal.set(Calendar.MINUTE, 0);
        
        s.execute(schedule, new DateTime(cal.getTimeInMillis()));

        assertThat(s.getCurrentPeriod(ts).name).as("period for " + cal.getTime()).isEqualTo("evening");

        cal.set(Calendar.HOUR_OF_DAY, 21);
        cal.set(Calendar.MINUTE, 0);
        
        s.execute(schedule, new DateTime(cal.getTimeInMillis()));

        assertThat(s.getCurrentPeriod(ts).name).as("period for " + cal.getTime()).isEqualTo("evening");
    }

    @Test
    public void testSchedule2() {
        
        ThreadContext.push("testSchedule2");
        
        try {
        
            checkSchedule2(renderSchedule2(), new Scheduler());
            
        } finally {
            ThreadContext.pop();
        }
    }

    private void checkSchedule2(Map<Thermostat, SortedMap<Period, ZoneStatus>> schedule, Scheduler s) {

        Entry<Thermostat, SortedMap<Period, ZoneStatus>> entry = schedule.entrySet().iterator().next();
        Thermostat ts = entry.getKey();
                
        Calendar cal = getMondayStart();

        s.execute(schedule, new DateTime(cal.getTimeInMillis()));

        assertThat(s.getCurrentPeriod(ts).name).as("period for " + cal.getTime()).isEqualTo("background");

        cal.set(Calendar.HOUR_OF_DAY, 8);
        cal.set(Calendar.MINUTE, 30);
        
        s.execute(schedule, new DateTime(cal.getTimeInMillis()));

        assertThat(s.getCurrentPeriod(ts).name).as("period for " + cal.getTime()).isEqualTo("morning");

        cal.set(Calendar.HOUR_OF_DAY, 9);
        cal.set(Calendar.MINUTE, 0);
        
        s.execute(schedule, new DateTime(cal.getTimeInMillis()));

        assertThat(s.getCurrentPeriod(ts).name).as("period for " + cal.getTime()).isEqualTo("day");

        cal.set(Calendar.HOUR_OF_DAY, 10);
        cal.set(Calendar.MINUTE, 0);
        
        s.execute(schedule, new DateTime(cal.getTimeInMillis()));

        assertThat(s.getCurrentPeriod(ts).name).as("period for " + cal.getTime()).isEqualTo("day");

        cal.set(Calendar.HOUR_OF_DAY, 15);
        cal.set(Calendar.MINUTE, 0);
        
        s.execute(schedule, new DateTime(cal.getTimeInMillis()));

        assertThat(s.getCurrentPeriod(ts).name).as("period for " + cal.getTime()).isEqualTo("background");

        cal.set(Calendar.HOUR_OF_DAY, 18);
        cal.set(Calendar.MINUTE, 0);
        
        s.execute(schedule, new DateTime(cal.getTimeInMillis()));

        assertThat(s.getCurrentPeriod(ts).name).as("period for " + cal.getTime()).isEqualTo("evening");

        cal.set(Calendar.HOUR_OF_DAY, 21);
        cal.set(Calendar.MINUTE, 0);
        
        s.execute(schedule, new DateTime(cal.getTimeInMillis()));

        assertThat(s.getCurrentPeriod(ts).name).as("period for " + cal.getTime()).isEqualTo("evening");
    }

    /**
     * @see <a href="https://github.com/home-climate-control/dz/issues/13">Once setpoint is changed, the zone never returns to schedule</a>
     */
    @Test
    public void testSchedule3() {
        
        ThreadContext.push("testSchedule3");
        
        try {
        
            checkSchedule3(renderSchedule3(), new Scheduler());
            
        } finally {
            ThreadContext.pop();
        }
    }

    private void checkSchedule3(Map<Thermostat, SortedMap<Period, ZoneStatus>> schedule, Scheduler s) {

        Entry<Thermostat, SortedMap<Period, ZoneStatus>> entry = schedule.entrySet().iterator().next();
        Thermostat ts = entry.getKey();
                
        Calendar cal = getMondayStart();

        s.execute(schedule, new DateTime(cal.getTimeInMillis()));

        assertThat(s.getCurrentPeriod(ts)).as("period for " + cal.getTime()).isNull();

        cal.set(Calendar.HOUR_OF_DAY, 9);
        cal.set(Calendar.MINUTE, 0);
        
        s.execute(schedule, new DateTime(cal.getTimeInMillis()));

        assertThat(s.getCurrentPeriod(ts).name).as("period for " + cal.getTime()).isEqualTo("morning");

        cal.set(Calendar.HOUR_OF_DAY, 15);
        cal.set(Calendar.MINUTE, 0);
        
        s.execute(schedule, new DateTime(cal.getTimeInMillis()));

        assertThat(s.getCurrentPeriod(ts)).as("period for " + cal.getTime()).isNull();

        cal.set(Calendar.HOUR_OF_DAY, 18);
        cal.set(Calendar.MINUTE, 0);
        
        s.execute(schedule, new DateTime(cal.getTimeInMillis()));

        assertThat(s.getCurrentPeriod(ts).name).as("period for " + cal.getTime()).isEqualTo("evening");

        cal.set(Calendar.HOUR_OF_DAY, 21);
        cal.set(Calendar.MINUTE, 0);
        
        s.execute(schedule, new DateTime(cal.getTimeInMillis()));

        assertThat(s.getCurrentPeriod(ts).name).as("period for " + cal.getTime()).isEqualTo("evening");

        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 0);
        
        s.execute(schedule, new DateTime(cal.getTimeInMillis()));

        assertThat(s.getCurrentPeriod(ts)).as("period for " + cal.getTime()).isNull();
    }

    @Test
    public void testSchedule4() {
        
        ThreadContext.push("testSchedule4");
        
        try {
        
            checkSchedule4(renderSchedule4(), new Scheduler());
            
        } finally {
            ThreadContext.pop();
        }
    }

    private void checkSchedule4(Map<Thermostat, SortedMap<Period, ZoneStatus>> schedule, Scheduler s) {

        Entry<Thermostat, SortedMap<Period, ZoneStatus>> entry = schedule.entrySet().iterator().next();
        Thermostat ts = entry.getKey();
                
        Calendar cal = getMondayStart();
        
        // Start the day

        s.execute(schedule, new DateTime(cal.getTimeInMillis()));
        
        // Has to be at +29C, enabled, voting - according to schedule

        assertThat(ts.getSetpoint()).as("setpoint for " + cal.getTime()).isEqualTo(29.0);

        // Change the setpoint to 26C manually
        
        ts.set(new ZoneStatusImpl(26, 0, true, true));

        assertThat(ts.getSetpoint()).as("setpoint for " + cal.getTime()).isEqualTo(26.0);

        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 10);
        
        s.execute(schedule, new DateTime(cal.getTimeInMillis()));

        assertThat(s.getCurrentPeriod(ts).name).as("period for " + cal.getTime()).isEqualTo("away");

        // Has to be still at 26C

        assertThat(ts.getSetpoint()).as("setpoint for " + cal.getTime()).isEqualTo(26.0);

        cal.set(Calendar.HOUR_OF_DAY, 1);
        cal.set(Calendar.MINUTE, 10);
        
        s.execute(schedule, new DateTime(cal.getTimeInMillis()));
        
        // 'reset' should've taken over by now
        
        // VT: FIXME: but it didn't - see the FIXME at Scheduler#execute for explanation.
        
        // VT: FIXME: Enable the assertion below when #13 is fixed
        // assertEquals("Wrong period for " + cal.getTime(), "reset", s.getCurrentPeriod(ts).name);

        // Has to be at 29C now
        
        // VT: FIXME: Enable the assertion below when #13 is fixed
        // assertEquals("Wrong setpoint for " + cal.getTime(), 29.0, ts.getSetpoint());
    }

    @Test
    public void testSchedule5() {
        
        ThreadContext.push("testSchedule5");
        
        try {
        
            checkSchedule5(renderSchedule5(), new Scheduler());
            
        } finally {
            ThreadContext.pop();
        }
    }

    private void checkSchedule5(Map<Thermostat, SortedMap<Period, ZoneStatus>> schedule, Scheduler s) {

        Entry<Thermostat, SortedMap<Period, ZoneStatus>> entry = schedule.entrySet().iterator().next();
        Thermostat ts = entry.getKey();
                
        Calendar cal = getMondayStart();
        
        // Start the day

        s.execute(schedule, new DateTime(cal.getTimeInMillis()));
        
        // Has to be at +29C, enabled, voting - according to schedule

        assertThat(ts.getSetpoint()).as("setpoint for " + cal.getTime()).isEqualTo(29.0);

        // Change the setpoint to 26C manually
        
        ts.set(new ZoneStatusImpl(26, 0, true, true));

        assertThat(ts.getSetpoint()).as("setpoint for " + cal.getTime()).isEqualTo(26.0);

        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 10);
        
        s.execute(schedule, new DateTime(cal.getTimeInMillis()));

        assertThat(s.getCurrentPeriod(ts).name).as("period for " + cal.getTime()).isEqualTo("away");

        // Has to be still at 26C

        assertThat(ts.getSetpoint()).as("setpoint for " + cal.getTime()).isEqualTo(26.0);

        cal.set(Calendar.HOUR_OF_DAY, 1);
        cal.set(Calendar.MINUTE, 10);
        
        s.execute(schedule, new DateTime(cal.getTimeInMillis()));
        
        // 'reset' should've taken over by now

        assertThat(s.getCurrentPeriod(ts).name).as("period for " + cal.getTime()).isEqualTo("reset");

        // Has to be at 29.5C now

        assertThat(ts.getSetpoint()).as("setpoint for " + cal.getTime()).isEqualTo(29.5);
    }

    @Test
    public void testDeviation() {
        
        ThreadContext.push("testDeviation");
        
        try {
            
            final Map<Thermostat, SortedMap<Period, ZoneStatus>> schedule = renderSchedule3();
            
            ScheduleUpdater u = new ScheduleUpdater() {
                
                @Override
                public Map<Thermostat, SortedMap<Period, ZoneStatus>> update() throws IOException {

                    return schedule;
                }
            };
            
            Scheduler s = new Scheduler(u);
            
            s.setScheduleGranularity(50);
            
            // Need this so the internal schedule is initialized
            
            s.start(0);
            
            Thread.sleep(50);
            
            // Stopping the scheduler doesn't clear the schedule
            s.stop();
            
            checkDeviation(schedule, s);
            
        } catch (InterruptedException ex) {

            throw new IllegalStateException(ex);
            
        } finally {
            ThreadContext.pop();
        }
    }
    
    private void checkDeviation(Map<Thermostat, SortedMap<Period, ZoneStatus>> schedule, Scheduler s) {
        
        Calendar cal = getMondayStart();

        {
            // This thermostat is not in the schedule
            Thermostat ts = new NullThermostat("alien");

            Deviation d = s.getDeviation(ts, 22, true, true, new DateTime(cal.getTimeInMillis()));

            assertThat(d.setpoint).as("setpoint deviation").isEqualTo(0.0, within(0.001));
        }
        
        Entry<Thermostat, SortedMap<Period, ZoneStatus>> entry = schedule.entrySet().iterator().next();
        Thermostat ts = entry.getKey();
                
        {
            // No period is present at this time
            
            s.execute(schedule, new DateTime(cal.getTimeInMillis()));

            Deviation d = s.getDeviation(ts, 22, false, false, new DateTime(cal.getTimeInMillis()));

            assertThat(d.setpoint).as("setpoint deviation").isEqualTo(0.0);
            assertThat(d.enabled).as("enabled deviation").isEqualTo(false);
            assertThat(d.voting).as("voting deviation").isEqualTo(false);
        }
        
        {
            cal.set(Calendar.HOUR_OF_DAY, 9);
            cal.set(Calendar.MINUTE, 0);

            s.execute(schedule, new DateTime(cal.getTimeInMillis()));

            Deviation d = s.getDeviation(ts, 22, true, true, new DateTime(cal.getTimeInMillis()));

            assertThat(d.setpoint).as("setpoint deviation").isEqualTo(-2.5);
            assertThat(d.enabled).as("enabled deviation").isEqualTo(false);
            assertThat(d.voting).as("voting deviation").isEqualTo(false);
        }
        
        {
            // No period is present at this time
            
            cal.set(Calendar.HOUR_OF_DAY, 12);
            cal.set(Calendar.MINUTE, 0);

            s.execute(schedule, new DateTime(cal.getTimeInMillis()));

            Deviation d = s.getDeviation(ts, 22, true, true, new DateTime(cal.getTimeInMillis()));

            assertThat(d.setpoint).as("setpoint deviation").isEqualTo(0.0);
            assertThat(d.enabled).as("enabled deviation").isEqualTo(false);
            assertThat(d.voting).as("voting deviation").isEqualTo(false);
        }
        
        {
            cal.set(Calendar.HOUR_OF_DAY, 18);
            cal.set(Calendar.MINUTE, 0);

            s.execute(schedule, new DateTime(cal.getTimeInMillis()));

            Deviation d = s.getDeviation(ts, 22, true, false, new DateTime(cal.getTimeInMillis()));

            assertThat(d.setpoint).as("setpoint deviation").isEqualTo(-2.8, within(0.001));
            assertThat(d.enabled).as("enabled deviation").isEqualTo(false);
            assertThat(d.voting).as("voting deviation").isEqualTo(true);
        }

        {
            cal.set(Calendar.HOUR_OF_DAY, 21);
            cal.set(Calendar.MINUTE, 0);

            s.execute(schedule, new DateTime(cal.getTimeInMillis()));

            Deviation d = s.getDeviation(ts, 24.8, false, true, new DateTime(cal.getTimeInMillis()));

            assertThat(d.setpoint).as("setpoint deviation").isEqualTo(0.0);
            assertThat(d.enabled).as("enabled deviation").isEqualTo(true);
            assertThat(d.voting).as("voting deviation").isEqualTo(false);
        }

        {
            cal.set(Calendar.HOUR_OF_DAY, 22);
            cal.set(Calendar.MINUTE, 0);

            s.execute(schedule, new DateTime(cal.getTimeInMillis()));

            Deviation d = s.getDeviation(ts, 24.8, true, true, new DateTime(cal.getTimeInMillis()));

            assertThat(d.setpoint).as("setpoint deviation").isEqualTo(0.0);
            assertThat(d.enabled).as("enabled deviation").isEqualTo(false);
            assertThat(d.voting).as("voting deviation").isEqualTo(false);
        }
        {
            // No period is present at this time
            
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 0);

            s.execute(schedule, new DateTime(cal.getTimeInMillis()));

            Deviation d = s.getDeviation(ts, 22, true, true, new DateTime(cal.getTimeInMillis()));

            assertThat(d.setpoint).as("setpoint deviation").isEqualTo(0.0);
        }
    }

    private static class NullThermostat implements Thermostat {

        private final Logger logger = LogManager.getLogger(getClass());

        private final String name;
        private Double setpoint;
        private Boolean enabled;
        private Boolean voting;
        
        public NullThermostat(String name) {
            
            this.name = name;
        }

        @Override
        public String getName() {

            return name;
        }

        @Override
        public double getSetpoint() {
            
            return setpoint;
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
            
            logger.info(getName() + ": set() to " + status);

            this.setpoint = status.getSetpoint();
            this.enabled = status.isOn();
            this.voting = status.isVoting();
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

            return enabled;
        }

        @Override
        public boolean isVoting() {

            return voting;
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
        
        @Override
        public String toString() {
            
            return getName();
        }
    }
}
