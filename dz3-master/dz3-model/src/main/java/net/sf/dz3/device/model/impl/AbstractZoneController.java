package net.sf.dz3.device.model.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import net.sf.dz3.device.model.Thermostat;
import net.sf.dz3.device.model.ThermostatSignal;
import net.sf.dz3.device.model.ThermostatStatus;
import net.sf.dz3.device.model.ZoneController;
import net.sf.jukebox.datastream.logger.impl.DataBroadcaster;
import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.datastream.signal.model.DataSink;
import net.sf.jukebox.jmx.JmxDescriptor;
import net.sf.jukebox.logger.LogAware;
import net.sf.jukebox.util.MessageDigestFactory;

import org.apache.log4j.NDC;

/**
 * The zone controller abstraction.
 *
 * Implements the behavior common for all the zone controller, and provides
 * the template methods for the rest.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2012
 */
public abstract class AbstractZoneController extends LogAware implements ZoneController {

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
    private final Map<String, Thermostat> name2ts = new TreeMap<String, Thermostat>();

    /**
     * All thermostats that are unhappy, including ones that are not {@link ThermostatStatus#isVoting() voting}.
     */
    protected final Map<Thermostat, ThermostatSignal> unhappy = new TreeMap<Thermostat, ThermostatSignal>();

    /**
     * Thermostats that are both unhappy and {@link ThermostatStatus#isVoting() voting}.
     */
    protected final Map<Thermostat, ThermostatSignal> unhappyVoting = new TreeMap<Thermostat, ThermostatSignal>();

    /**
     * Mapping from the thermostat to its current failure condition.
     *
     * As soon as a valid signal arrives from the thermostat, its entry is
     * removed from this map.
     */
    protected Map<Thermostat, ThermostatSignal> failed = new TreeMap<Thermostat, ThermostatSignal>();

    /**
     * Last known signal map.
     */
    protected final Map<Thermostat, ThermostatSignal> lastKnownSignal = new TreeMap<Thermostat, ThermostatSignal>();

    private final DataBroadcaster<Double> dataBrodacaster = new DataBroadcaster<Double>();

    /**
     * Zone controller output signal, computed in {@link #stateChanged(Thermostat, ThermostatSignal)}.
     */
    private DataSample<Double> signal;

    /**
     * Create an instance with no connected thermostats.
     * 
     * @param name Zone controller name.
     */
    public AbstractZoneController(String name) {

        this(name, null);
    }

    /**
     * Create an instance with connected thermostats.
     * 
     * @param name Zone controller name.
     * @param sources Thermostats to use as signal sources.
     */
    public AbstractZoneController(String name, Set<Thermostat> sources) {


        this.name = name;
        signature = new MessageDigestFactory().getMD5(name).substring(0, 19);

        signal = new DataSample<Double>(System.currentTimeMillis(), name, signature, 0d, null);

        if (sources != null) {

            for (Iterator<Thermostat> i = sources.iterator(); i.hasNext(); ) {

                Thermostat source = i.next();
                source.addConsumer(this);

                logger.info("Consumer: " + source.getName() + ": " + source);
                name2ts.put(source.getName(), source);
            }
        }
    }

    @Override
    public void consume(DataSample<ThermostatSignal> signal) {

        assert(signal != null);

        Thermostat source = name2ts.get(signal.sourceName);

        if (source == null) {
            throw new IllegalArgumentException("Don't know anything about '" + signal.sourceName + "'");
        }

        stateChanged(source, signal.sample);
    }

    /**
     * {@inheritDoc}
     */
    private synchronized void stateChanged(Thermostat source, ThermostatSignal pv) {

        NDC.push("stateChanged");

        try {

            logger.debug("Source: " + source);
            logger.debug("Signal: " + pv);

            if (lastKnownSignal.get(source) == null) {

                // Let's pretend the signal didn't change, this should trigger the downflow correctly
                lastKnownSignal.put(source, pv);
            }

            checkError(source, pv);
            boolean needBump = checkUnhappy(source, pv);

            signal = computeDemand(pv.demand.timestamp, needBump);

            stateChanged();

            if (needBump) {
                raise(source);
            }

        } finally {
            lastKnownSignal.put(source, pv);
            NDC.pop();
        }
    }

    /**
     * Execute {@link Thermostat#raise() on every thermostat for this zone other than {@code source}.
     * 
     * @param source Thermostat to exclude from the {@code raise()}.
     */
    private void raise(Thermostat source) {

        Set<Thermostat> tsSet = new TreeSet<Thermostat>(lastKnownSignal.keySet());

        // It's already calling, no need to raise() it
        tsSet.remove(source);

        for (Iterator<Thermostat> i = tsSet.iterator(); i.hasNext(); ) {
            i.next().raise();
        }
    }

    private void checkError(Thermostat source, ThermostatSignal signal) {

        if (signal.demand.isError()) {

            // Faulty thermostat can't participate in the process,
            // its zone will be handled"by default" - damper
            // controller will take care of that

            unhappy.remove(source);
            unhappyVoting.remove(source);

            if (!failed.containsKey(source)) {

                // This is a fresh failure
                logger.error("FIXME: process the error:" + signal);
                failed.put(source, signal);
            }

        } else {

            if (failed.containsKey(source)) {

                // Failure has cleared
                logger.info("Cleared failure condition for " + source);
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

        NDC.push("checkUnhappy");

        try {

            int calling = countCalling(unhappyVoting.values());

            // Old signal is no longer relevant
            unhappy.remove(source);
            unhappyVoting.remove(source);

            if (signal.demand.isError()) {

                // Faulty thermostat can't participate in the process,
                // its zone will be handled"by default" - damper
                // controller will take care of that
                //
                // (This should've been taken care of by checkError(), but
                // let's be paranoid)
                return false;
            }

            if (signal.calling) {

                unhappy.put(source, signal);

                if (signal.voting) {

                    unhappyVoting.put(source, signal);

                    // Now let's see if the bump is required

                    if (calling == 0) {

                        // Yep, that's the first voting thermostat

                        logger.info("This HVAC run was initiated by " + source.getName());

                        return true;
                    }
                }
            }


            return false;

        } finally {
            NDC.pop();
        }
    }

    /**
     * 
     * @param signalSet Set of thermostat signals to count the calling status for.
     * 
     * @return Number of signals with calling bit set.
     */
    private int countCalling(Collection<ThermostatSignal> signalSet) {

        int count = 0;

        for (Iterator<ThermostatSignal> i = signalSet.iterator(); i.hasNext(); ) {

            ThermostatSignal signal = i.next();

            count += signal.calling ? 1 : 0;
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

        NDC.push("computeDemand");

        try {

            if (Double.compare(signal.sample, 0d) == 0 && !needBump) {

                // Nobody's calling yet, demand is irrelevant
                return new DataSample<Double>(timestamp, name, signature, 0d, null);
            }

            double demandVoting = 0;

            // Calculate demand for voting zones only for now

            for (Iterator<Entry<Thermostat, ThermostatSignal>> i = unhappyVoting.entrySet().iterator(); i.hasNext();) {

                Entry<Thermostat, ThermostatSignal> entry = i.next();
                ThermostatSignal signal = entry.getValue();

                demandVoting += signal.demand.sample;
            }

            logger.debug("Voting demand: " + demandVoting);

            // Let's see what non-voting zones say

            double demandTotal = 0;

            for (Iterator<Entry<Thermostat, ThermostatSignal>> i = unhappy.entrySet().iterator(); i.hasNext();) {

                Entry<Thermostat, ThermostatSignal> entry = i.next();
                ThermostatSignal signal = entry.getValue();

                demandTotal += signal.demand.sample;
            }

            logger.debug("Total demand: " + demandVoting);

            // Bigger demand value wins
            // Just make sure voting and non-voting demand point in the same direction

            if (demandVoting * demandTotal >= 0 && Math.abs(demandTotal) > Math.abs(demandVoting)) {

                logger.debug("Final demand: " + demandTotal);
                return new DataSample<Double>(timestamp, name, signature, demandTotal, null);

            } else {

                logger.debug("Final demand: " + demandVoting);
                return new DataSample<Double>(timestamp, name, signature, demandVoting, null);
            }

        } finally {
            NDC.pop();
        }
    }

    private void stateChanged() {

        dataBrodacaster.broadcast(signal);
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();

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

    public synchronized DataSample<Double> getSignal() {

        return signal;
    }

    public void addConsumer(DataSink<Double> consumer) {

        dataBrodacaster.addConsumer(consumer);
    }

    public void removeConsumer(DataSink<Double> consumer) {

        dataBrodacaster.removeConsumer(consumer);
    }

    public JmxDescriptor getJmxDescriptor() {

        return new JmxDescriptor(
                "dz",
                "Zone Controller",
                name,
                "Analyzes thermostat outputs and decides what to tell to the Unit");
    }
}
