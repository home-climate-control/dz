package net.sf.dz3r.scheduler;

import com.homeclimatecontrol.hcc.model.SchedulePeriod;
import com.homeclimatecontrol.hcc.model.ZoneSettings;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.SortedMap;

/**
 * Abstraction to support back end independent schedule update.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public interface ScheduleUpdater {

    /**
     * Keep the schedule up to date.
     *
     * @return Flux of (zone name, schedule period to zone settings mapping) pairs.
     */
    Flux<Map.Entry<String, SortedMap<SchedulePeriod, ZoneSettings>>> update();
}
