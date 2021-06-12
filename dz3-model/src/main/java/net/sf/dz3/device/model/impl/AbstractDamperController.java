package net.sf.dz3.device.model.impl;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import com.homeclimatecontrol.jukebox.jmx.JmxAttribute;
import com.homeclimatecontrol.jukebox.jmx.JmxAware;
import com.homeclimatecontrol.jukebox.sem.SemaphoreGroup;
import net.sf.dz3.device.actuator.Damper;
import net.sf.dz3.device.model.DamperController;
import net.sf.dz3.device.model.Thermostat;
import net.sf.dz3.device.model.ThermostatSignal;
import net.sf.dz3.device.model.Unit;
import net.sf.dz3.device.model.UnitSignal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Base logic for the damper controller.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public abstract class AbstractDamperController implements DamperController, JmxAware {

    protected final Logger logger = LogManager.getLogger();

    /**
     * Association from a thermostat to a damper.
     */
    protected final Map<Thermostat, Damper> ts2damper = new HashMap<>();

    /**
     * Association from a thermostat to its last known signal.
     */
    protected final Map<Thermostat, ThermostatSignal> ts2signal = new TreeMap<>();

    /**
     * Last known unit signal.
     */
    protected DataSample<UnitSignal> hvacSignal = null;

    /**
     * Instrumentation map.
     *
     * The only purpose is to be accessed via JMX ({@link #getDamperMap()}). Content is not used in any calculations.
     * Value is refreshed in {@link #shuffle(Map)}.
     */
    private final Map<Damper, Double> lastMap = new HashMap<>();

    /**
     * Thermostat signal consumer.
     */
    private final ThermostatListener tsListener = new ThermostatListener();

    /**
     * Mapping from thermostat name to thermostat instance - needed to support the {@link #tsListener}.
     */
	private final Map<String, Thermostat> name2ts = new TreeMap<>();

	/**
     * Stays {@code true} until {@link #powerOff} is called.
     *
     * If this flag is {@code false} (i.e. {@link #powerOff} was called), all other methods will
     * throw {@link IllegalStateException} upon invocation
     */
    private boolean enabled = true;

    /**
     * Create an instance with nothing attached.
     */
    protected AbstractDamperController() {

    }

    /**
     * Create an instance and make it listen to the unit and thermostats.
     *
     * @param unit Unit to listen to.
     * @param ts2damper Thermostats to listen to and dampers to associate them with.
     */
    protected AbstractDamperController(Unit unit, Map<Thermostat, Damper> ts2damper) {

        if (unit == null) {
            throw new IllegalArgumentException("unit can't be null");
        }

        if (ts2damper == null) {
            throw new IllegalArgumentException("ts2damper can't be null");
        }

        unit.addConsumer(this);

        for (Map.Entry<Thermostat, Damper> td : ts2damper.entrySet()) {

            var ts = td.getKey();
            var d = td.getValue();

            put(ts, d);

            ts.addConsumer(tsListener);

            name2ts.put(ts.getName(), ts);
        }
    }

    @Override
    public synchronized void put(Thermostat ts, Damper damper) {
        ts2damper.put(ts, damper);
    }

    @Override
    public synchronized void remove(Thermostat ts) {
        ts2damper.remove(ts);
    }

    public synchronized void stateChanged(Thermostat source, ThermostatSignal signal) {

        ThreadContext.push("signalChanged");

        try {

            checkEnabled();

            if (!ts2damper.containsKey(source)) {
                throw new IllegalArgumentException("Don't know anything about " + source);
            }

            ts2signal.put(source, signal);
            logger.info("Demand: {}={}", source.getName(), signal.demand.sample);
            logger.info("ts2signal.size()={}", ts2signal.size());

            sync();

        } finally {
            ThreadContext.pop();
        }

    }

    @Override
    public synchronized void consume(DataSample<UnitSignal> signal) {

        ThreadContext.push("consume");

        try {

            checkEnabled();

            if (signal == null) {
                throw new IllegalArgumentException("signal can't be null");
            }

            logger.info("UnitSignal: {}", signal.sample);

            if (this.hvacSignal == null) {

                if (signal.sample.running) {

                    // It would be realistic to assume it's been off, right?
                    logger.info("Turning ON");

                    sync();

                } else {

                    // Might've been killed last time, need to set the dampers straight
                    park();
                }

            } else if (!this.hvacSignal.sample.running && signal.sample.running) {

                logger.info("Turning ON");
                sync();

            } else if (this.hvacSignal.sample.running && !signal.sample.running) {

                park();

            } else {

                // No change except for recalculating the damper positions
                sync();
            }


        } finally {
            this.hvacSignal = signal;
            ThreadContext.pop();
        }
    }

    /**
     * Set positions of dampers in the map.
     *
     * @param damperMap Key is the damper, value is the position to set.
     */
    private void shuffle(Map<Damper, Double> damperMap) {

        ThreadContext.push("shuffle");

        try {

            logger.info("damperMap.size()={}", damperMap.size());

            for (Map.Entry<Damper, Double> dp : damperMap.entrySet()) {

                var damper = dp.getKey();
                var position = dp.getValue();

                try {

                    logger.info("damper position: {}={}", damper.getName(), position);

                    damper.set(position);

                } catch (IOException ex) {

                    // This can be really bad, for it's possible that all the dampers
                    // are controlled by the same controller and it's the controller that is faulty.
                    // Don't want the HVAC to suffocate with all the dampers closed.

                    logger.fatal("Can't set the damper position for {}", damper, ex);
                }
            }

        } finally {

            lastMap.clear();
            lastMap.putAll(damperMap);

            ThreadContext.pop();
        }
    }

    /**
     * Get the damper position map for instrumentation purposes.
     *
     * @return Damper positions as array of strings.
     */
    @JmxAttribute(description = "Damper positions")
    public synchronized String[] getDamperMap() {

        var result = new String[lastMap.size()];
        var resultMap = new TreeMap<String, Double>();

        for (Map.Entry<Damper, Double> dp : lastMap.entrySet()) {

            var damper = dp.getKey();
            Double position = dp.getValue();

            resultMap.put(damper.getName(), position);
        }

        var offset = 0;
        for (Map.Entry<String, Double> np : resultMap.entrySet()) {

            var name = np.getKey();
            var position = np.getValue();

            result[offset++] = name + "=" + position;
        }

        return result;
    }

    /**
     * Recalculate the damper state according to [possibly] changed internal state.
     */
    protected final void sync() {

        // VT: NOTE: This assumes compute() is stateless, ideally, it should stay that way.
        // If there is ever a need to make it stateful, compute() should be called outside
        // of the fork and the map passed to shuffle) within.

        if (this.hvacSignal != null && this.hvacSignal.sample.running) {

            shuffle(compute());

        } else {

            park();
        }
    }

    private void checkEnabled() {
        if (!enabled) {
            throw new IllegalStateException("powerOff() was called already");
        }
    }

    private SemaphoreGroup park() {

        ThreadContext.push("park");

        try {

            logger.info("Turning OFF");

            // This can't be handled like a regular setPosition(), there may be custom parking positions
            // and damper multiplexers - everything must be parked.
            var parked = new SemaphoreGroup();

            for (Map.Entry<Thermostat, Damper> td : ts2damper.entrySet()) {
                parked.add(td.getValue().park());
            }

            return parked;

        } finally {
            ThreadContext.pop();
        }
    }

    @Override
    public final synchronized void powerOff() {

        ThreadContext.push("powerOff");

        try {

            enabled = false;
            logger.warn("Powering off");

            // Can't use normal park() here, for it is asynchronous and this method
            // must finish everything before returning

            try {

                park().waitForAll();
                logger.info("Parked dampers");

            } catch (InterruptedException ex) {
                logger.warn("Failed to park all dampers, some may be in a wrong position", ex);
                Thread.currentThread().interrupt();
            }

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Compute damper positions based on known data.
     */
    protected abstract Map<Damper, Double> compute();

    private class ThermostatListener implements DataSink<ThermostatSignal> {

		@Override
		public void consume(DataSample<ThermostatSignal> signal) {

            if ((signal == null)) {
                throw new IllegalArgumentException("signal can't be null");
            }

			var source = name2ts.get(signal.sourceName);

			if (source == null) {
				throw new IllegalArgumentException("Don't know anything about '" + signal.sourceName + "'");
			}

			stateChanged(source, signal.sample);
		}
    }
}
