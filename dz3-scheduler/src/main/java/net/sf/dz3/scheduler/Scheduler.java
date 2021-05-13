package net.sf.dz3.scheduler;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.EmptyStackException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.sf.dz3.device.model.Thermostat;
import net.sf.dz3.device.model.ZoneStatus;
import net.sf.dz3.device.model.impl.ZoneStatusImpl;
import com.homeclimatecontrol.jukebox.jmx.JmxAttribute;
import com.homeclimatecontrol.jukebox.jmx.JmxAware;
import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;
import com.homeclimatecontrol.jukebox.sem.ACT;
import com.homeclimatecontrol.jukebox.sem.EventSemaphore;
import com.homeclimatecontrol.jukebox.service.StoppableService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.joda.time.DateTime;

/**
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko</a> 2001-2018
 */
public class Scheduler implements Runnable, StoppableService, JmxAware {

    private final Logger logger = LogManager.getLogger(getClass());
    private final static DecimalFormat df = new DecimalFormat("#0.0###;-#0.0###");
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ScheduleUpdater updater;
    
    /**
     * Schedule check and execution granularity, in milliseconds.
     */
    private long scheduleGranularityMillis = 60 * 1000; 
    
    /**
     * The schedule.
     */
    private final Map<Thermostat, SortedMap<Period, ZoneStatus>> schedule = new TreeMap<Thermostat, SortedMap<Period, ZoneStatus>>();
    
    /**
     * Current settings.
     * 
     * VT: NOTE: It is possible to get rid of this using {@link ZoneStatusImpl#equals(Object)}
     * implementation, but let's think of it later, premature optimization is the mother of all evil.
     */
    private final Map<Thermostat, ZoneStatus> currentStatus = new TreeMap<Thermostat, ZoneStatus>();
    
    /**
     * Mapping of selected period to a thermostat.
     */
    private final Map<Thermostat, Period> currentPeriod = new TreeMap<Thermostat, Period>();
    
    /**
     * Create an instance using no updater with empty schedule.
     */
    public Scheduler() {
        
        this(null, null);
    }
    
    /**
     * Create an instance using no updater and initialize it with a given schedule.
     * 
     * @param schedule Schedule to initialize with.
     */
    public Scheduler(Map<Thermostat, SortedMap<Period, ZoneStatus>> schedule) {
        
        this(null, schedule);
    }
        
    /**
     * Create an instance using a given updater and rely on updater to fetch the schedule.
     * 
     * @param updater Updater to use.
     */
    public Scheduler(ScheduleUpdater updater) {
        
        this(updater, null);
    }

    /**
     * Create an instance using a given updater and initialize it with a given schedule.
     * 
     * @param updater Updater to use.
     * @param schedule Schedule to initialize with.
     */
    public Scheduler(ScheduleUpdater updater, Map<Thermostat, SortedMap<Period, ZoneStatus>> schedule) {
        
        this.updater = updater;

        if (schedule == null) {
            logger.warn("schedule is null, ignored");
            return;
        }

        this.schedule.putAll(schedule);
    }

    /**
     * Start with default delay.
     * 
     * This method needs to be called in order for the scheduler to start functioning.
     * @return 
     * 
     * @see #start(long)
     */
    public EventSemaphore start() {

        // There has to be some initial delay to let sensors settle,
        // otherwise there'll be NullPointerExceptions everywhere
        
        // VT: FIXME: I don't like the statement above, it suggest flakiness. Let's see
        // if this restriction can be removed.

        return start(10000);
    }

    /**
     * Start with a given initial delay.
     * 
     * This method needs to be called in order for the scheduler to start functioning.
     * 
     * @param initialDelayMillis Initial delay.
     * 
     * @see #start()
     */
    public EventSemaphore start(long initialDelayMillis) {
        
        logger.warn("VT: FIXME: Synchronize to the minute boundary");
        
        scheduler.scheduleAtFixedRate(this, initialDelayMillis, getScheduleGranularity(), TimeUnit.MILLISECONDS);

        // We're cheating, for simplicity
        
        ACT started = new ACT();
        
        started.post();
        
        return started;
    }
    
    /**
     * Stop the scheduler.
     * 
     * @return
     */
    public EventSemaphore stop() {
        
        ThreadContext.push("stop");
        
        try {
            
            logger.info("stopping");

            scheduler.shutdown();
            
            try {
            
                scheduler.awaitTermination(10000, TimeUnit.MILLISECONDS);
                
            } catch (InterruptedException ex) {
                
                logger.warn("Failed to shut down the scheduler cleanly", ex);
            }
            
            logger.info("stopped");

            // We're cheating, for simplicity
            
            ACT stopped = new ACT();
            
            stopped.post();
            
            return stopped;
            
        } finally {
            ThreadContext.pop();
        }
    }
    
    /**
     * @return {@link #scheduleGranularityMillis}.
     */
    @JmxAttribute(description = "Schedule check and execution granularity, in milliseconds")
    public long getScheduleGranularity() {
        
        return scheduleGranularityMillis;
    }
    
    /**
     * @param scheduleGranularityMillis Schedule granularity, in milliseconds.
     */
    public void setScheduleGranularity(long scheduleGranularityMillis) {
        
        if (scheduleGranularityMillis <= 0) {
        
            throw new IllegalArgumentException(scheduleGranularityMillis + ": value doesn't make sense");
        }
        
        // VT: FIXME: Currently, this will not be honored if invoked after the scheduler is started.
        // Good enough for testing purposes.
        
        this.scheduleGranularityMillis = scheduleGranularityMillis;
    }
    
    /**
     * This method is to be executed every {@link #getScheduleGranularity()} milliseconds.
     * 
     * VT: FIXME: It is a bad idea to have this method exposed, need to move the whole thing into an inner class after the test coverage is sufficient
     */
    public void run() {
        
        ThreadContext.push("run");
        
        try {

            logger.info("Checking schedule");
            
            update(schedule);
            execute(schedule, new DateTime());
            
        } catch (Throwable t) {
          
            // If an exception is not caught, the executor will choke and never call us again
            logger.error("Unexpected exception", t);
         
        } finally {
            
            logger.info("done");
            
            ThreadContext.pop();
            
            // Even though the pool size is one, this would be a safe thing to do
            // and won't incur a performance penalty - it's executed less than
            // once in a minute
            ThreadContext.clearStack();
        }
    }
    
    /**
     * Update the schedule.
     */
    private void update(final Map<Thermostat, SortedMap<Period, ZoneStatus>> target) {

        ThreadContext.push("update");
        
        try {
            
            if (updater == null) {
            
                logger.debug("No updater specified, doing nothing");
                return;
            }
            
            Map<Thermostat, SortedMap<Period, ZoneStatus>> newSchedule = updater.update();
            
            if (newSchedule == null) {
                
                // Third party developers may want to implement different schedule updaters,
                // and even though the design contract requires not to return null,
                // they might.
                
                // Note that this exception doesn't break the whole scheduler because it runs asynchronously.
                
                throw new IllegalStateException("Bad updater implementation returned null");
            }

            // Completely discard the current schedule and replace it with the new one,
            // there's no need to use rocket science here
            
            target.clear();
            target.putAll(newSchedule);
            
        } catch (IOException ex) {
            
            logger.error("Schedule update failed", ex);
            
        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Match the schedule against current time and execute necessary changes.
     * 
     * The only reason this method is public is to make it testable.
     * 
     * @param target Schedule to apply.
     * @param when Time to use when applying the schedule, in milliseconds.
     */
    public void execute(final Map<Thermostat, SortedMap<Period, ZoneStatus>> target, DateTime when) {

        ThreadContext.push("execute");
        
        try {
            
            for (Iterator<Entry<Thermostat, SortedMap<Period, ZoneStatus>>> i = target.entrySet().iterator(); i.hasNext(); ) {
                
                Entry<Thermostat, SortedMap<Period, ZoneStatus>> entry = i.next();
                Thermostat ts = entry.getKey();
                SortedMap<Period, ZoneStatus> zoneSchedule = entry.getValue();
                
                try {

                    execute(ts, zoneSchedule, when);

                } catch (Throwable t) {

                    // Errors with individual thermostats shouldn't affect others
                    logger.error(ts.getName() + ": failed to set schedule, will retry on next run", t);
                }
            }
            
        } finally {
            ThreadContext.pop();
        }
    }
    
    /**
     * Find and execute the schedule for the given zone.
     * 
     * @param ts Thermostat to control.
     * @param zoneSchedule Schedule to use.
     * @param time Time to match against.
     */
    private void execute(Thermostat ts, SortedMap<Period, ZoneStatus> zoneSchedule, DateTime time) {
        
        ThreadContext.push("execute");
        
        try {
            
            ThreadContext.push("(" + ts.getName() + ")");
            
            try {
            
                PeriodMatcher periodMatcher = new PeriodMatcher();
                Period p = periodMatcher.match(zoneSchedule, time);
                ZoneStatus status = zoneSchedule.get(p);
                ZoneStatus currentZoneStatus = currentStatus.get(ts);
                
                // VT: FIXME: https://github.com/home-climate-control/dz/issues/13
                
                // The check below will only enforce the next event if it is different
                // from the previous. Otherwise, if the setpoint or voting status were
                // changed manually, the thermostat will stay at current settings.
                
                // Workaround: avoid overlapping/adjacent events with identical settings.

                if (!status.equals(currentZoneStatus)) {

                    ts.set(status);
                    currentStatus.put(ts, status);
                    currentPeriod.put(ts, p);

                    logger.info(ts.getName() + " set to " + status);
                }

            } finally {
                ThreadContext.pop();
            }
            
        } catch (EmptyStackException ex) {
            
            logger.info(ts.getName() + ": no active period found");
            
            currentStatus.remove(ts);
            currentPeriod.remove(ts);
            
        } finally {
            logger.info("done");
            ThreadContext.pop();
        }
    }
    
    /**
     * Get the currently selected status for the given thermostat.
     * 
     * @param ts Thermostat to get the status for.
     * @return Currently selected status for the given thermostat, or {@code null} if there's none.
     */
    public ZoneStatus getCurrentStatus(Thermostat ts) {
        
        return currentStatus.get(ts);
    }
    
    /**
     * Get the currently selected period for the given thermostat.
     * 
     * @param ts Thermostat to get the period for.
     * @return Currently selected period for the given thermostat, or {@code null} if there's none.
     */
    public Period getCurrentPeriod(Thermostat ts) {
        
        return currentPeriod.get(ts);
    }

    @Override
    public JmxDescriptor getJmxDescriptor() {
        
        return new JmxDescriptor(
                "dz",
                "Scheduler",
                Integer.toHexString(hashCode()),
                "Changes thermostat settings based on a schedule");
    }

    /**
     * Determine deviations between current and scheduled values.
     *
     * @param ts Thermostat to determine deviation for.
     * @param setpointTemperature Actual setpoint temperature.
     * @param currentEnabled Actual "enabled" value.
     * @param currentVoting Actual "voting" value.
     * @param time Time to perform the calculation for.
     * 
     * @return An object containing deviations found, if any. 
     */
    public Deviation getDeviation(Thermostat ts, double setpointTemperature, boolean currentEnabled, boolean currentVoting, DateTime time) {
        
        ThreadContext.push("getDeviation(" + ts.getName() + ")");
        
        try {

            try {
                
                SortedMap<Period, ZoneStatus> zoneSchedule = schedule.get(ts);
                
                if (zoneSchedule == null) {
                    
                    logger.debug("No schedule found for " + ts.getName() + " (yet?)");
                    return new Deviation(0, false, false);
                }
                
                PeriodMatcher periodMatcher = new PeriodMatcher();
                Period p = periodMatcher.match(zoneSchedule, time);
                ZoneStatus statusScheduled = zoneSchedule.get(p);

                // VT: FIXME: Dump priority should be taken into consideration as well
                ZoneStatus statusCurrent = new ZoneStatusImpl(setpointTemperature, 0, currentEnabled, currentVoting);
                
                if (statusScheduled.equals(statusCurrent)) {
                    
                    logger.debug("on schedule");
                    return new Deviation(0, false, false);
                }

                Deviation result = new Deviation(
                        statusCurrent.getSetpoint() - statusScheduled.getSetpoint(),
                        statusCurrent.isOn() != statusScheduled.isOn(),
                        statusCurrent.isVoting() != statusScheduled.isVoting());
                
                logger.debug("Scheduled: " + statusScheduled);
                logger.debug("Actual:    " + statusCurrent);
                logger.debug("Deviation: " + result);
                
                return result;
                
            } catch (EmptyStackException ex) {
                
                logger.info(ts.getName() + ": no active period found");
                return new Deviation(0, false, false);
            }
            
        } finally {
            ThreadContext.pop();
        }
    }

    public static class Deviation {
        
        public final double setpoint;
        public final boolean enabled;
        public final boolean voting;
        
        public Deviation(double setpoint, boolean enabled, boolean voting) {
            
            this.setpoint = setpoint;
            this.enabled = enabled;
            this.voting = voting;
        }
        
        public String toString() {
            
            StringBuilder sb = new StringBuilder();
            
            sb.append("(setpoint deviation=").append(df.format(setpoint));
            sb.append(enabled ? ", enabled differs" : "");
            sb.append(voting ? ", voting differs" : "");
            sb.append(")");
            
            return sb.toString();
        }
    }
}
