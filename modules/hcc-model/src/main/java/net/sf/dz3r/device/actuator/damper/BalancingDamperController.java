package net.sf.dz3r.device.actuator.damper;

import net.sf.dz3r.model.Zone;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.hvac.ZoneStatus;
import org.apache.logging.log4j.ThreadContext;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Balancing damper controller, supports modulating dampers.
 *
 * If bang/bang dampers is all you have, use {@link SimpleDamperController} instead.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class BalancingDamperController extends AbstractDamperController {

    /**
     * When do we start thinking about dumping excess static pressure.
     *
     * If the total area of open dampers vs. all the dampers is more than
     * this, no big deal. Otherwise, we start opening the dump zone dampers
     * and otherwise fiddle with them.
     *
     * The closer is this value to 1.0, the better from the A/C unit point
     * of view. However, this has to be modified for the case of the
     * variable speed fan, it is not yet clear how, though.
     *
     * Rough model shows that the dump threshold more than 0.4 is
     * unacceptable from the comfort point of view. In real life, the dump
     * threshold may be even lower because the dampers are not ideal and
     * leak a lot.
     */
    protected double dumpThreshold = 0.3;

    protected BalancingDamperController(Map<Zone, Damper<?>> zone2damper) {
        super(zone2damper);
    }

    @Override
    protected Map<Damper<?>, Double> compute(Map<String, Signal<ZoneStatus, String>> zone2signal) {

        ThreadContext.push("compute");
        try {

            logger.debug("zone2signal={}", zone2signal);

            var damper2position = new TreeMap<Damper<?>, Double>();
            var demand2zone = new TreeMap<Double, Set<String>>();

            zone2signal.forEach((zoneName, zoneSignal) -> {

                if (zoneSignal.isError()) {

                    // We have no idea what temperature this zone is at, let's assume the worst case
                    var damper = getDamperFor(zoneName);
                    damper2position.put(damper, damper.getParkPosition());

                    return;
                }

                // Negative demand counts as 0, otherwise damper positions will go below 0 and go boom
                var demand = Math.max(0.0, zoneSignal.getValue().callingStatus().demand);
                var zoneSet = demand2zone.computeIfAbsent(demand, k -> new TreeSet<>());

                zoneSet.add(zoneName);
            });

            logger.debug("demand2zone: {}", demand2zone);

            double most = demand2zone.isEmpty() ? 1 : demand2zone.lastKey();

            // Normalize

            double scale = 1 / most;

            // See if most was 0
            // Have to compare to all extremes due to funky nature of division by zero

            scale = Double.compare(scale, Double.NaN) == 0 ? 0 : scale;
            scale = Double.compare(scale, Double.POSITIVE_INFINITY) == 0 ? 0 : scale;
            scale = Double.compare(scale, Double.NEGATIVE_INFINITY) == 0 ? 0 : scale;

            logger.debug("scale={}", scale);

            // Now, (signal + offset) * scale should be a value for the
            // damper position.

            // Shuffle the dampers

            var finalScale = scale; // need "effectively final"

            demand2zone.forEach((demand, zoneSet) -> {
                zoneSet.forEach(zone -> {
                    damper2position.put(getDamperFor(zone), demand * finalScale);
                });
            });

            logger.debug("result={}", damper2position);

            return damper2position;

        } finally {
            ThreadContext.pop();
        }
    }
}
