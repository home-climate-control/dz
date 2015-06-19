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
        
        Scheduler s1 = new Scheduler();
        Scheduler s2 = new Scheduler(schedule);
        Scheduler s3 = new Scheduler(updater);
        Scheduler s4 = new Scheduler(updater, null);
    }
}
