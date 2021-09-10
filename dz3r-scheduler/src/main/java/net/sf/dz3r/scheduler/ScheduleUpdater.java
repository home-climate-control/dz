package net.sf.dz3r.scheduler;

import net.sf.dz3r.model.Zone;
import net.sf.dz3r.model.ZoneSettings;

import java.io.IOException;
import java.util.Map;
import java.util.SortedMap;

/**
 * Abstraction to support back end independent schedule update.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
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
    Map<Zone, SortedMap<SchedulePeriod, ZoneSettings>> update() throws IOException;
}
