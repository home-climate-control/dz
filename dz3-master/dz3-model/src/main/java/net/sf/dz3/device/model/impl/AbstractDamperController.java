package net.sf.dz3.device.model.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.logging.log4j.ThreadContext;

import net.sf.dz3.device.actuator.Damper;
import net.sf.dz3.device.model.DamperController;
import net.sf.dz3.device.model.Thermostat;
import net.sf.dz3.device.model.ThermostatSignal;
import net.sf.dz3.device.model.Unit;
import net.sf.dz3.device.model.UnitSignal;
import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.datastream.signal.model.DataSink;
import net.sf.jukebox.jmx.JmxAttribute;
import net.sf.jukebox.jmx.JmxAware;
import net.sf.jukebox.logger.LogAware;
import net.sf.servomaster.device.model.TransitionStatus;

/**
 * Base logic for the damper controller.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2018
 */
public abstract class AbstractDamperController extends LogAware implements DamperController, JmxAware {

    /**
     * Completion service for asynchronous transitions.
     *
     * This pool requires exactly one thread.
     */
    CompletionService<Future<TransitionStatus>> transitionCompletionService = new ExecutorCompletionService<>(Executors.newFixedThreadPool(1));

    /**
     * Association from a thermostat to a damper.
     */
    protected final Map<Thermostat, Damper> ts2damper = new HashMap<Thermostat, Damper>();
    
    /**
     * Association from a thermostat to its last known signal.
     */
    protected final Map<Thermostat, ThermostatSignal> ts2signal = new TreeMap<Thermostat, ThermostatSignal>();
    
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
    private final Map<Damper, Double> lastMap = new HashMap<Damper, Double>();
    
    /**
     * Thermostat signal consumer.
     */
    private final ThermostatListener tsListener = new ThermostatListener();
    
    /**
     * Mapping from thermostat name to thermostat instance - needed to support the {@link #tsListener}.
     */
	private final Map<String, Thermostat> name2ts = new TreeMap<String, Thermostat>();

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
    public AbstractDamperController() {
        
    }
    
    /**
     * Create an instance and make it listen to the unit and thermostats.
     * 
     * @param unit Unit to listen to.
     * @param ts2damper Thermostats to listen to and dampers to associate them with.
     */
    public AbstractDamperController(Unit unit, Map<Thermostat, Damper> ts2damper) {
        
        if (unit == null) {
            throw new IllegalArgumentException("unit can't be null");
        }
        
        if (ts2damper == null) {
            throw new IllegalArgumentException("ts2damper can't be null");
        }
        
        unit.addConsumer(this);
        
        for (Iterator<Thermostat> i = ts2damper.keySet().iterator(); i.hasNext(); ) {

        	Thermostat ts = i.next();
            Damper d = ts2damper.get(ts);
            
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

    public synchronized Future<TransitionStatus> stateChanged(Thermostat source, ThermostatSignal signal) {

        ThreadContext.push("signalChanged");

        try {
            
            checkEnabled();
            
            if (!ts2damper.containsKey(source)) {
                throw new IllegalArgumentException("Don't know anything about " + source);
            }
            
            ts2signal.put(source, signal);
            logger.info("Demand: " + source.getName() + "=" + signal.demand.sample);
            logger.info("ts2signal.size()=" + ts2signal.size());
            
            return sync();

        } finally {
            ThreadContext.pop();
        }
    }

    public synchronized void consume(DataSample<UnitSignal> signal) {
        
        ThreadContext.push("consume");
        
        try {
            
            checkEnabled();
            
            if (signal == null) {
                throw new IllegalArgumentException("signal can't be null");
            }
            
            logger.info("UnitSignal: " + signal.sample);
            
            if (this.hvacSignal == null) {
                
                if (signal.sample.running) {
                    
                    // It would be realistic to assume it's been off, right?
                    logger.info("Turning ON");

                    sync();
                    
                } else {

                    // Might've been killed last time, need to set the dampers straight
                    park(true);
                }
                
            } else if (!this.hvacSignal.sample.running && signal.sample.running) {

                logger.info("Turning ON");

                sync();

            } else if (this.hvacSignal.sample.running && !signal.sample.running) {
                
                park(true);

            } else {

                // No change except for recalculating the damper positions
                sync();
            }
            

        } finally {
            this.hvacSignal = signal;
            ThreadContext.pop();
        }
    }
    
    private Future<TransitionStatus> park(boolean async) {

        ThreadContext.push("park");
        
        try {
            
            logger.info("Turning OFF");

            Map<Damper, Double> damperMap = new HashMap<Damper, Double>(); 

            for (Iterator<Thermostat> i = ts2damper.keySet().iterator(); i.hasNext(); ) {

                Thermostat ts = i.next();
                Damper d = ts2damper.get(ts);

                damperMap.put(d, d.getParkPosition());
            }

            Future<TransitionStatus> done = shuffle(damperMap, async);

            logger.info("parked");

            return done;

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Set positions of dampers in the map.
     *
     * Normally, the positions will be set asynchronously. The only exception to this is
     * when {@code shuffle()} is called via {@link #park()} from {@link #powerOff()}.
     *
     * @param damperMap Key is the damper, value is the position to set.
     * @param async {@code true} if the positions are to be set asynchronously.
     */
    private Future<TransitionStatus> shuffle(Map<Damper, Double> damperMap, boolean async) {
        
        ThreadContext.push("shuffle");
        
        try {

            transitionCompletionService.submit(new Damper.MoveGroup(damperMap, async));

            try {

                // VT: NOTE: The following line unwraps one level of Future. The first Future
                // is completed when the transitions have been fired, and the second is
                // when they all complete.

                return transitionCompletionService.take().get();

            } catch (InterruptedException | ExecutionException ex) {

                // VT: FIXME: Oops... Really don't know what to do with this, will have to collect stats
                // before this can be reasonably handled

                throw new IllegalStateException("Unhandled exception", ex);
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
     
        String[] result = new String[lastMap.size()];
        
        Map<String, Double> resultMap = new TreeMap<String, Double>();
        
        for (Iterator<Damper> i = lastMap.keySet().iterator(); i.hasNext(); ) {
            
            Damper d = i.next();
            Double position = lastMap.get(d);
            
            resultMap.put(d.getName(), position);
        }
        
        int offset = 0;
        for (Iterator<String> i = resultMap.keySet().iterator(); i.hasNext(); ) {
            
            String name = i.next();
            Double position = resultMap.get(name);
            
            result[offset++] = name + "=" + position; 
        }
        
        return result;
    }
    
    /**
     * Recalculate the damper state according to [possibly] changed internal state.
     */
    protected final Future<TransitionStatus> sync() {
        
        // VT: NOTE: This assumes compute() is stateless, ideally, it should stay that way.
        // If there is ever a need to make it stateful, compute() should be called outside
        // of the fork and the map passed to shuffle) within.
        
        if (this.hvacSignal != null && this.hvacSignal.sample.running) {
        
            return shuffle(compute(), true);
            
        } else {
            
            return park(true);
        }
    }

    private void checkEnabled() {
        if (!enabled) {
            throw new IllegalStateException("powerOff() was called already");
        }
    }

    @Override
    public final synchronized void powerOff() {
        
        ThreadContext.push("powerOff");
        
        try {

            enabled = false;
            logger.warn("Powering off");
            
            park(false);
            
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

            assert(signal != null);

            Thermostat source = name2ts.get(signal.sourceName);

            if (source == null) {
                throw new IllegalArgumentException("Don't know anything about '" + signal.sourceName + "'");
            }

            stateChanged(source, signal.sample);
        }
    }
}
