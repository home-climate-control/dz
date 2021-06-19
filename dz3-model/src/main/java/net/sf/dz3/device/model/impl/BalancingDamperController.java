package net.sf.dz3.device.model.impl;

import com.homeclimatecontrol.jukebox.jmx.JmxAttribute;
import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;
import net.sf.dz3.device.actuator.Damper;
import net.sf.dz3.device.model.Thermostat;
import net.sf.dz3.device.model.ThermostatSignal;
import net.sf.dz3.device.model.Unit;
import org.apache.logging.log4j.ThreadContext;

import java.util.HashMap;
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

    /**
     * Create an instance with nothing attached.
     */
    public BalancingDamperController() {

    }

    /**
     * Create an instance and make it listen to the unit and thermostats.
     *
     * @param unit Unit to listen to.
     * @param ts2damper Thermostats to listen to and dampers to associate them with.
     */
    public BalancingDamperController(Unit unit, Map<Thermostat, Damper> ts2damper) {
        super(unit, ts2damper);
    }

    /**
     * Get the dump threshold.
     *
     * @return Current value of the dump threshold.
     */
    @JmxAttribute(description = "Dump threshold")
    public final double getDumpThreshold() {
        return dumpThreshold;
    }

    /**
     * Set the dump threshold.
     *
     * @param dumpThreshold Dump threshold to set.
     *
     * @exception IllegalArgumentException if the parameter value is outside of 0...1.0 range.
     */
    public void setDumpThreshold(double dumpThreshold) {

        if ( dumpThreshold < 0 || dumpThreshold > 1 ) {
            throw new IllegalArgumentException("Invalid value " + dumpThreshold + " (should be in 0..1 range)");
        }

        this.dumpThreshold = dumpThreshold;

        sync();
    }

    @Override
    protected Map<Damper, Double> compute() {

        ThreadContext.push("compute");

        try {

            // Calculate the mapping between the thermostat signal and the
            // damper position

            // Readjust the throttles:
            //
            // - Room with the most demand gets 1.0
            // - Room with the least demand gets 0.0
            // - All the rest line up accordingly

            // VT: FIXME: Implement dump threshold logic

            var demand2ts = new TreeMap<Double, Set<Thermostat>>();
            var damperMap = new HashMap<Damper, Double>();

            for (Map.Entry<Thermostat, ThermostatSignal> t2s : ts2signal.entrySet()) {

                var ts = t2s.getKey();
                var signal = t2s.getValue();

                if (signal.demand.isError()) {

                    // We have no idea what temperature this zone is at,
                    // let's assume the worst case

                    var d = ts2damper.get(ts);
                    damperMap.put(d, d.getParkPosition());

                    continue;
                }

                // Negative demand counts as 0, otherwise damper positions will go
                // below 0 - boom
                var demand = Double.valueOf(signal.demand.sample >= 0.0 ? signal.demand.sample : 0);
                var tsSet = demand2ts.computeIfAbsent(demand, k -> new TreeSet<>());

                tsSet.add(ts);
            }

            logger.debug("demand2ts: {}", demand2ts);

            double most = demand2ts.isEmpty() ? 1 : demand2ts.lastKey();

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

            for (Map.Entry<Double, Set<Thermostat>> d2ts : demand2ts.entrySet()) {

                var demand = d2ts.getKey();
                var tsSet = d2ts.getValue();
                var value = demand * scale;

                for (Thermostat ts : tsSet) {
                    damperMap.put(ts2damper.get(ts), value);
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
                "Balancing Damper Controller (modulating dampers)");
    }
}
