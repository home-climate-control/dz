package net.sf.dz3r.device.actuator.damper;

import net.sf.dz3r.model.Zone;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.hvac.ZoneStatus;

import java.util.Map;
import java.util.TreeMap;

/**
 * Simple damper controller, supports bang/bang dampers only.
 *
 * If bang/bang dampers is all you have, there's no sense to go beyond this.
 * If you have modulating dampers, use {@link BalancingDamperController} instead.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class SimpleDamperController extends AbstractDamperController {

    protected SimpleDamperController(Map<Zone, Damper<?>> zone2damper) {
        super(zone2damper);
    }

    @Override
    protected Map<Damper<?>, Double> compute(Map<String, Signal<ZoneStatus, String>> zone2signal) {

        logger.debug("zone2signal={}", zone2signal);

        var result = new TreeMap<Damper<?>, Double>();

        zone2signal.forEach((key, value) -> {

            var damper = getDamperFor(key);
            var position = value.getValue().callingStatus().calling() ? 1d : 0d;

            result.put(damper, position);
        });

        logger.debug("result={}", result);

        return result;
    }
}
