package net.sf.dz3.device.model.impl;

import com.homeclimatecontrol.jukebox.datastream.logger.impl.DataBroadcaster;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;
import net.sf.dz3.device.model.Thermostat;
import net.sf.dz3.device.model.ThermostatSignal;
import net.sf.dz3.device.model.ThermostatStatus;
import net.sf.dz3.device.model.ZoneController;
import net.sf.dz3.util.digest.MessageDigestCache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * The zone controller abstraction.
 *
 * Implements the behavior common for all the zone controller, and provides
 * the template methods for the rest.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public abstract class AbstractZoneController implements ZoneController {

    protected final Logger logger = LogManager.getLogger();

    /**
     * Zone controller name.
     *
     * Necessary evil to allow instrumentation signature.
     */
    private final String name;

    /**
     * Instrumentation signature.
     */
    private final String signature;

    /**
     * Mapping from thermostat name to thermostat instance - needed to support the {@link #dataBrodacaster}.
     */
    private final Map<String, Thermostat> name2ts = new TreeMap<>();

    /**
     * All thermostats that are unhappy, including ones that are not {@link ThermostatStatus#isVoting() voting}.
     */
    protected final Map<Thermostat, ThermostatSignal> unhappy = new TreeMap<>();

    /**
     * Thermostats that are both unhappy and {@link ThermostatStatus#isVoting() voting}.
     */
    protected final Map<Thermostat, ThermostatSignal> unhappyVoting = new TreeMap<>();

    /**
     * Mapping from the thermostat to its current failure condition.
     *
     * As soon as a valid signal arrives from the thermostat, its entry is
     * removed from this map.
     */
    protected Map<Thermostat, ThermostatSignal> failed = new TreeMap<>();

    /**
     * Last known signal map.
     */
    protected final Map<Thermostat, ThermostatSignal> lastKnownSignal = new TreeMap<>();

    private final DataBroadcaster<Double> dataBrodacaster = new DataBroadcaster<>();

    /**
     * Zone controller output signal, computed in {@link #stateChanged(Thermostat, ThermostatSignal)}.
     */
    private DataSample<Double> signal;

    /**
     * Create an instance with no connected thermostats.
     *
     * @param name Zone controller name.
     */
    protected AbstractZoneController(String name) {
        this(name, null);
    }

    /**
     * Create an instance with connected thermostats.
     *
     * @param name Zone controller name.
     * @param sources Thermostats to use as signal sources.
     */
    protected AbstractZoneController(String name, Set<Thermostat> sources) {

        this.name = name;
        signature = MessageDigestCache.getMD5(name).substring(0, 19);

        signal = new DataSample<>(System.currentTimeMillis(), name, signature, 0d, null);

        if (sources != null) {

            for (Thermostat source : sources) {

                source.addConsumer(this);

                logger.info("Consumer: {}: {}", source.getName(), source);
                name2ts.put(source.getName(), source);
            }
        }
    }

    @Override
    public void consume(DataSample<ThermostatSignal> signal) {

        if ((signal == null)) {
            throw new IllegalArgumentException("signal can't be null");
        }

        Thermostat source = name2ts.get(signal.sourceName);

        if (source == null) {
            throw new IllegalArgumentException("Don't know anything about '" + signal.sourceName + "'");
        }

        stateChanged(source, signal.sample);
    }

    private synchronized void stateChanged(Thermostat source, ThermostatSignal pv) {

        ThreadContext.push("stateChanged");

        try {

            if (logger.isTraceEnabled()) {

                // DataSample.toString() is expensive,and DataSample is a component of ThermostatSignal

                logger.trace("Source: {}", source);
                logger.trace("Signal: {}", pv);
            }

            // Let's pretend the signal didn't change, this should trigger the downflow correctly
            lastKnownSignal.putIfAbsent(source, pv);

            checkError(source, pv);
            boolean needBump = checkUnhappy(source, pv);

            signal = computeDemand(pv.demand.timestamp, needBump);

            stateChanged();

            if (needBump) {
                raise(source);
            }

        } finally {
            lastKnownSignal.put(source, pv);
            ThreadContext.pop();
        }
    }

    /**
     * Execute {@link Thermostat#raise() on every thermostat for this zone other than {@code source}.
     *
     * @param source Thermostat to exclude from the {@code raise()}.
     */
    private void raise(Thermostat source) {

        var tsSet = new TreeSet<>(lastKnownSignal.keySet());

        // It's already calling, no need to raise() it
        tsSet.remove(source);

        for (Thermostat thermostat : tsSet) {
            thermostat.raise();
        }
    }

    /**
     * Remove the thermostat from {@link #unhappy} and {@link #unhappyVoting} if error,
     * add to {@link #failed} if a fresh failure, remove otherwise, log the actions.
     *
     * @param source Thermostat to check the signal for.
     * @param signal Signal value.
     */
    private void checkError(Thermostat source, ThermostatSignal signal) {

        if (signal.demand.isError()) {

            // Faulty thermostat can't participate in the process,
            // its zone will be handled "by default" - damper
            // controller will take care of that

            unhappy.remove(source);
            unhappyVoting.remove(source);

            if (!failed.containsKey(source)) { // NOSONAR Need the log message

                // This is a fresh failure
                logger.error("FIXME: process the error: {}", signal);
                failed.put(source, signal);
            }

        } else {

            if (failed.containsKey(source)) { // NOSONAR Need the log message

                // Failure has cleared
                logger.info("Cleared failure condition for {}", source);
                failed.remove(source);
            }
        }
    }

    /**
     * See whether the thermostat is still calling.
     *
     * @param source Thermostat whose signal is being considered.
     * @param signal Thermostat signal.
     *
     * @return {@code true} if this signal indicates a need to bump the HVAC
     * into "running" state (and possibly other thermostats into "calling" state).
     */
    private boolean checkUnhappy(Thermostat source, ThermostatSignal signal) {

        ThreadContext.push("checkUnhappy");

        try {

            int calling = countCalling(unhappyVoting.values());

            // Old signal is no longer relevant
            unhappy.remove(source);
            unhappyVoting.remove(source);

            if (signal.demand.isError()) {
                throw new IllegalStateException("this should've been handled by checkError()");
            }

            if (signal.calling) {

                unhappy.put(source, signal);

                if (signal.voting || isLastEnabled(source)) {

                    unhappyVoting.put(source, signal);

                    // Now let's see if the bump is required

                    if (calling == 0) {

                        // Yep, that's the first voting thermostat
                        logger.info("This HVAC run was initiated by {}", source.getName());

                        return true;
                    }
                }
            }


            return false;

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Check if this thermostat is the last enabled thermostat.
     *
     * @param ts Thermostat to check.
     *
     * @return {@code true} if the given thermostat is the last one enabled.
     */
    private boolean isLastEnabled(Thermostat ts) {

        var enabled = name2ts
                .values()
                .stream()
                .filter(Thermostat::isOn)
                .collect(Collectors.toSet());

        if (enabled.size() > 1) {
            return false;
        }

        return enabled.iterator().next() == ts;
    }

    /**
     *
     * @param signalSet Set of thermostat signals to count the calling status for.
     *
     * @return Number of signals with calling bit set.
     */
    private int countCalling(Collection<ThermostatSignal> signalSet) {

        var count = 0;

        for (var thermostatSignal : signalSet) {
            count += thermostatSignal.calling ? 1 : 0;
        }

        return count;
    }

    /**
     * Compute the total zone controller demand.
     *
     * @param timestamp Last signal's timestamp.
     * @param needBump {@code true} if the unit needs to be kicked on.
     *
     * @return Total demand for this zone controller.
     */
    private DataSample<Double> computeDemand(long timestamp, boolean needBump) {

        ThreadContext.push("computeDemand");

        try {

            if (Double.compare(signal.sample, 0d) == 0 && !needBump) {
                // Nobody's calling yet, demand is irrelevant
                return new DataSample<>(timestamp, name, signature, 0d, null);
            }

            double demandVoting = 0;

            // Calculate demand for voting zones only for now

            for (var entry : unhappyVoting.entrySet()) {
                demandVoting += entry.getValue().demand.sample;
            }

            logger.debug("Voting demand: {}", demandVoting);

            // Let's see what non-voting zones say

            double demandTotal = 0;

            for (var entry : unhappy.entrySet()) {

                var thermostatSignal = entry.getValue();

                demandTotal += thermostatSignal.demand.sample;
            }

            logger.debug("Total demand: {}", demandVoting);

            // Bigger demand value wins
            // Just make sure voting and non-voting demand point in the same direction

            if (demandVoting * demandTotal >= 0 && Math.abs(demandTotal) > Math.abs(demandVoting)) {

                logger.debug("Final demand: {}", demandTotal);
                return new DataSample<>(timestamp, name, signature, demandTotal, null);

            } else {

                logger.debug("Final demand: {}", demandVoting);
                return new DataSample<>(timestamp, name, signature, demandVoting, null);
            }

        } finally {
            ThreadContext.pop();
        }
    }

    private void stateChanged() {
        dataBrodacaster.broadcast(signal);
    }

    @Override
    public String toString() {

        var sb = new StringBuilder();

        sb.append("<");
        renderString(sb);
        sb.append(">");

        return sb.toString();
    }

    protected void renderString(StringBuilder sb) {

        sb.append("signals: ").append(lastKnownSignal).append(", ");

        if (!failed.isEmpty()) {
            sb.append("failed: ").append(failed).append(", ");
        }

        sb.append("unhappy: ").append(unhappy).append(", ");
        sb.append("unhappyVoting: ").append(unhappyVoting).append(", ");

        synchronized (this) {
            sb.append(signal);
        }
    }

    @Override
    public synchronized DataSample<Double> getSignal() {
        return signal;
    }

    @Override
    public void addConsumer(DataSink<Double> consumer) {
        dataBrodacaster.addConsumer(consumer);
    }

    @Override
    public void removeConsumer(DataSink<Double> consumer) {
        dataBrodacaster.removeConsumer(consumer);
    }

    @Override
    public JmxDescriptor getJmxDescriptor() {

        return new JmxDescriptor(
                "dz",
                "Zone Controller",
                name,
                "Analyzes thermostat outputs and decides what to tell to the Unit");
    }
}
