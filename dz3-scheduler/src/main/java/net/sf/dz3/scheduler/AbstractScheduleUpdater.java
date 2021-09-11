package net.sf.dz3.scheduler;

import net.sf.dz3.device.model.Thermostat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Back end independent base for the schedule updater.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public abstract class AbstractScheduleUpdater implements ScheduleUpdater {

    protected final Logger logger = LogManager.getLogger(getClass());

    /**
     * Set of thermostats to retrieve updated schedule for.
     */
    private final Map<String, Set<Thermostat>> targetMap = new TreeMap<>();

    /**
     * Create an instance.
     *
     * @param ts2source Keys are thermostats to update the schedule for,
     * values are source names to pull schedules from.
     */
    protected AbstractScheduleUpdater(Map<Thermostat, String> ts2source) {

        if (ts2source == null || ts2source.isEmpty()) {
            throw new IllegalStateException("No thermostats to control given, why bother?");
        }

        for (var entry : ts2source.entrySet()) {

            var ts = entry.getKey();
            var source = entry.getValue();

            if (source == null || "".equals(source)) {

                throw new IllegalArgumentException("Source name can't be null or empty (check entry for thermostat '" + ts.getName() + "')");
            }

            var tSet = targetMap.computeIfAbsent(source, v -> new TreeSet<Thermostat>());
            tSet.add(ts);
        }
    }

    /**
     * Get a thermostat instance by name.
     *
     * @param name Name to match.
     *
     * @return Thermostat instance, or {@code null} if not found.
     */
    protected final Set<Thermostat> getByName(String name) {
        return targetMap.get(name);
    }
}
