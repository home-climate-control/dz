package net.sf.dz3.device.model.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.logging.log4j.ThreadContext;

import net.sf.dz3.device.actuator.Damper;
import net.sf.dz3.device.model.Thermostat;
import net.sf.dz3.device.model.ThermostatSignal;
import net.sf.dz3.device.model.Unit;
import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;

/**
 * Simple damper controller, supports bang/bang dampers only.
 *
 * If bang/bang dampers is all you have, there's no sense to go beyond this.
 * If you have modulating dampers, use {@link BalancingDamperController} instead.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2020
 */
public class SimpleDamperController extends AbstractDamperController {

    /**
     * Create an instance and make it listen to the unit and thermostats.
     *
     * @param unit Unit to listen to.
     * @param ts2damper Thermostats to listen to and dampers to associate them with.
     */
    public SimpleDamperController(Unit unit, Map<Thermostat, Damper> ts2damper) {
        super(unit, ts2damper);
    }

    @Override
    Map<Damper, Double> compute(Map<Thermostat, Damper> ts2damper, Map<Thermostat, ThermostatSignal> ts2signal) {

        ThreadContext.push("compute");

        try {

            Map<Damper, Double> damperMap = new HashMap<>();

            for (Iterator<Thermostat> i = ts2damper.keySet().iterator(); i.hasNext(); ) {

                Thermostat ts = i.next();
                ThermostatSignal signal = ts2signal.get(ts);
                Damper d = ts2damper.get(ts);

                if (signal == null) {

                    // Quite possible we're just starting, assume demand is there
                    damperMap.put(d, d.getParkPosition());

                } else {

                    if (signal.demand.isError()) {

                        // We have no idea what temperature this zone is at,
                        // let's assume the worst case

                        damperMap.put(d, d.getParkPosition());

                    } else {

                        damperMap.put(d, signal.calling ? 1.0 : 0.0);
                    }
                }
            }

            return damperMap;

        } finally {
            ThreadContext.pop();
        }
    }

    @Override
    public JmxDescriptor getJmxDescriptor() {

        return new JmxDescriptor(
                "dz",
                getClass().getSimpleName(),
                Integer.toHexString(hashCode()),
                "Simple Damper Controller (bang-bang dampers only)");
    }
}
