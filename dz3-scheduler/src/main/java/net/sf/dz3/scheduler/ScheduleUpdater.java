package net.sf.dz3.scheduler;

import java.io.IOException;
import java.util.Map;
import java.util.SortedMap;

import net.sf.dz3.device.model.Thermostat;
import net.sf.dz3.device.model.ZoneStatus;

/**
 * Abstraction to support back end independent schedule update.
 *  
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2010
 */
public interface ScheduleUpdater {

    /**
     * Update the schedule.
     * 
     * @return New schedule. If there are no matching events found, empty map must be returned.
     * Must not return {@code null}. 
     * 
     * @throws IOException if things go wrong.
     */
    Map<Thermostat, SortedMap<Period, ZoneStatus>> update() throws IOException; 
}
