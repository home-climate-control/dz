package net.sf.dz3.scheduler;

import java.io.IOException;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import net.sf.dz3.device.model.Thermostat;
import net.sf.dz3.device.model.ZoneStatus;
import junit.framework.TestCase;

public class SchedulerTest extends TestCase {

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
    
    /**
     * Test the no-argument {@link Scheduler#start()} method.
     */
    public void testStart() {
        
        Scheduler s = new Scheduler();
        
        s.start();
    }

    public void testStartStop() {
        
        Scheduler s = new Scheduler();
        
        s.start(0);
    }
}
