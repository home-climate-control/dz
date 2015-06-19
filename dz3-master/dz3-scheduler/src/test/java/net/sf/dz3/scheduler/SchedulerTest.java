package net.sf.dz3.scheduler;

import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.NDC;

import net.sf.dz3.device.model.Thermostat;
import net.sf.dz3.device.model.ZoneStatus;
import net.sf.dz3.scheduler.Scheduler.Deviation;
import junit.framework.TestCase;

public class SchedulerTest extends TestCase {
    
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
}
