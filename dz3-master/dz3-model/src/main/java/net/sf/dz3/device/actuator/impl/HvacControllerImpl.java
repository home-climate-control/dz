package net.sf.dz3.device.actuator.impl;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.sf.dz3.device.actuator.HvacController;
import net.sf.dz3.device.actuator.HvacDriver;
import net.sf.dz3.device.model.HvacMode;
import net.sf.dz3.device.model.HvacSignal;
import net.sf.dz3.device.model.Unit;
import net.sf.dz3.device.model.UnitSignal;
import net.sf.jukebox.datastream.logger.impl.DataBroadcaster;
import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.datastream.signal.model.DataSink;
import net.sf.jukebox.datastream.signal.model.DataSource;
import net.sf.jukebox.jmx.JmxAttribute;
import net.sf.jukebox.jmx.JmxAware;
import net.sf.jukebox.jmx.JmxDescriptor;
import net.sf.jukebox.logger.LogAware;
import net.sf.jukebox.util.MessageDigestFactory;

import org.apache.log4j.NDC;

/**
 * Base class for HVAC hardware drivers.
 * 
 * Provides common functions - input sanity checks, mode switching, signal rebroadcasts, etc.
 *  
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2010
 */
public class HvacControllerImpl extends LogAware implements HvacController, JmxAware {
    
    /**
     * The unit name.
     */
    private final String name;

    /**
     * Instrumentation signature.
     */
    private final String signature;
    
    /**
     * HVAC hardware driver.
     */
    private final HvacDriver hvacDriver;

    private DataBroadcaster<HvacSignal> dataBroadcaster = new DataBroadcaster<HvacSignal>();
    
    /**
     * Last known state. 
     */
    private DataSample<HvacSignal> state;
    
    private final BlockingQueue<Runnable> commandQueue = new LinkedBlockingQueue<Runnable>();
    private final ThreadPoolExecutor executor;
    
    /**
     * Create a named instance that is not connected to anything and is off.
     */
    public HvacControllerImpl(String name, HvacDriver hvacDriver) {
    
        this(name, hvacDriver, "off", null);
    }
    
    /**
     * Create a named instance with nothing connected to it.
     * 
     * @param name Unit name.
     * @param mode Initial operating mode.
     */
    public HvacControllerImpl(String name, HvacDriver hvacDriver, String mode) {

        this(name, hvacDriver, mode, null);
    }

    /**
     * Create an instance that is listening to a given data source.
     * 
     * @param source Data source to listen to.
     */
    public HvacControllerImpl(String name, HvacDriver hvacDriver, String mode, DataSource<UnitSignal> source) {
        
        if (name == null || "".equals(name)) {
            throw new IllegalArgumentException("name can't be null or empty");
        }
        
        this.name = name;
        signature = new MessageDigestFactory().getMD5(name).substring(0, 19);
        
        if (hvacDriver == null) {
            throw new IllegalArgumentException("hvacDriver can't be null");
        }
        
        this.hvacDriver = hvacDriver;
    
        executor = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, commandQueue);
        
        // Shut it off in case it was left on by a dead process
        setMode(mode);
        hardwareSetRunning(false);
        hardwareSetDemand(0.0);

        if (source != null) {
            source.addConsumer(this);
        }
    }
    
    /**
     * Set initial mode.
     * 
     * This method should only be called from the constructor, the reason for its existence is to reduce clutter there.
     * 
     * @param mode Mode to set, as a string.
     */
    private void setMode(String mode) {
        
        if ("off".equalsIgnoreCase(mode)) {
            
            this.state = new DataSample<HvacSignal>(name, signature, new HvacSignal(HvacMode.OFF, 0.0, false, 0), null);
            
        } else if ("cooling".equalsIgnoreCase(mode)) {

            this.state = new DataSample<HvacSignal>(name, signature, new HvacSignal(HvacMode.COOLING, 0.0, false, 0), null);
            hardwareChangeMode(HvacMode.OFF, HvacMode.COOLING);
            
        } else if ("heating".equalsIgnoreCase(mode)) {
            
            this.state = new DataSample<HvacSignal>(name, signature, new HvacSignal(HvacMode.HEATING, 0.0, false, 0), null);
            hardwareChangeMode(HvacMode.OFF, HvacMode.HEATING);
            
        } else {
            
            throw new IllegalArgumentException("Unknown mode '" + mode + "', valid values are 'off', 'cooling' and 'heating'");
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final synchronized HvacMode getMode() {
        
        return state.sample.mode;
    }

    /**
     * {@inheritDoc}
     */
    public final synchronized void setMode(HvacMode mode) {
        
        NDC.push("setMode");
        
        try {
        
            if (this.state.sample.mode.equals(mode)) {
                // Do absolutely nothing
                return;
            }
            
            logger.info("Changing mode from " + this.state.sample.mode + " to " + mode);
            hardwareChangeMode(this.state.sample.mode, mode);
            
            state = new DataSample<HvacSignal>(System.currentTimeMillis(),
                    name,
                    signature,
                    new HvacSignal(mode, state.sample.demand, state.sample.running, state.sample.uptime), null);
            
            stateChanged(); 
        
        } finally {
            NDC.pop();
        }
    }

    /**
     * Switch the unit on or off.
     * 
     * This method should not be called more often than it is necessary.
     * Nevertheless, it must be idempotent to increase fault tolerance.
     * 
     * @param running Desired running mode.
     * @param timestamp 
     */
    private synchronized void setRunning(boolean running, long timestamp) {
        
        // VT: FIXME: uptime
        state = new DataSample<HvacSignal>(timestamp,
                name,
                signature,
                new HvacSignal(state.sample.mode, state.sample.demand, running, 0), null);
        
        hardwareSetRunning(running);
        stateChanged(); 
    }

    private synchronized void setDemand(double demand, long timestamp) {
        
        // VT: FIXME: uptime
        state = new DataSample<HvacSignal>(timestamp,
                name,
                signature,
                new HvacSignal(state.sample.mode, demand, state.sample.running, 0), null);
        
        hardwareSetDemand(demand);
        stateChanged(); 
    }

    /**
     * Change the HVAC hardware operating mode.
     * 
     * This operation must be asynchronous, return immediately and never throw any exceptions.
     * Whatever problems that may have been encountered must be reported via separate channels.
     * 
     * @param modeFrom Mode to change from.
     * @param modeTo Mode to change to.
     */
    private void hardwareChangeMode(HvacMode modeFrom, HvacMode modeTo) {
        
        executor.execute(new CommandChangeMode(hvacDriver, modeTo));
    }
    
    /**
     * Change hardware running state.
     * 
     * This operation must be asynchronous, return immediately and never throw any exceptions.
     * Whatever problems that may have been encountered must be reported via separate channels.
     * 
     * @param running {@code true} to start, {@code false} to stop.
     */
    private void hardwareSetRunning(boolean running) {
        
        executor.execute(new CommandSetRunning(hvacDriver, running));
    }
    
    /**
     * Pass the demand value to hardware. 
     * This operation must be asynchronous, return immediately and never throw any exceptions.
     * Whatever problems that may have been encountered must be reported via separate channels.
     * 
     * @param demand Demand to set.
     */
    private void hardwareSetDemand(double demand) {
        
        executor.execute(new CommandSetDemand(hvacDriver, demand));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String getName() {
        
        if ( name == null ) {

            throw new IllegalStateException("Not Initialized");
        }

        return name;
    }

    /**
     * Determine what to do with the HVAC hardware based on the command received.
     * 
     * @param signal Signal produced by {@link Unit}, with a twist (to be documented later).
     */
    @Override
    public final void consume(DataSample<UnitSignal> signal) {

        NDC.push("consume");
        
        try {
            
            check(signal);
            
            logger.debug("demand (old, new): (" + state.sample.demand + ", " + signal.sample + ")");
            
            if (state.sample.mode.equals(HvacMode.OFF)) {
                
                // No need to do anything at all
                logger.info("Unit is off, input ignored");
                
                return;
            }
            
            if (signal.sample.demand > 0 && !isRunning()) {
                
                logger.info("Turning ON");
                setRunning(true, signal.timestamp);
                
            } else if (signal.sample.demand == 0 && isRunning()) {
                
                logger.info("Turning OFF");
                setRunning(false, signal.timestamp);
                
            } else {
                
                logger.debug("no change");
            }
            
        } finally {
            
            setDemand(signal.sample.demand, signal.timestamp);
            NDC.pop();
        }
    }

    /**
     * Make sure the signal given to {@link #consume(DataSample)} is sane.
     * 
     * @param signal Signal to check.
     */
    private void check(DataSample<UnitSignal> signal) {
        
        NDC.push("check");

        try {

            if (signal == null) {
                throw new IllegalArgumentException("signal can't be null");
            }

            if (signal.isError()) {
                
                logger.error("Should not have propagated all the way here", signal.error);
                throw new IllegalArgumentException("Error signal should have been handled by zone controller");
            }
            
        } finally {
            NDC.pop();
        }
    }
    /**
     * Broadcast the state change.
     */
    private void stateChanged() {
        
        dataBroadcaster.broadcast(state);
    }
    /**
     * {@inheritDoc}
     */
    public final void addConsumer(DataSink<HvacSignal> consumer) {
        
        dataBroadcaster.addConsumer(consumer);
    }

    /**
     * {@inheritDoc}
     */
    public final void removeConsumer(DataSink<HvacSignal> consumer) {
        
        dataBroadcaster.removeConsumer(consumer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int compareTo(HvacController o) {

        return getName().compareTo(o.getName());
    }

    @JmxAttribute(description="Last Known Signal")
    public final HvacSignal getSignal() {
        
        return state.sample;
    }

    public final boolean isRunning() {
        return state.sample.running;
    }
    
    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();

        sb.append("AbstractHvacDriver(").append(name).append(", ");
        sb.append(getSignal());
        sb.append(")");

        return sb.toString();
    }

    @Override
    public JmxDescriptor getJmxDescriptor() {
        
        return new JmxDescriptor(
                "dz",
                "HVAC Controller",
                name,
                "Analyzes Unit output and issues commands to HVAC hardware driver");
    }
    
    private abstract class Command implements Runnable {

        protected final HvacDriver target;
        
        public Command(HvacDriver target) {
            this.target = target;
        }
        
        @Override
        public final void run() {

            int retry = 0;
            while (true) {
                
                NDC.push("run" + (retry > 0 ? "#retry-" + retry : ""));
                
                try {

                    logger.debug("Running " + toString());
                    
                    execute();
                    
                    logger.debug("Success");
                    return;

                } catch (Throwable t) {

                    logger.fatal("Failed to execute " + getClass().getSimpleName(), t);
                    
                    // We're going to retry this till the end of time, because
                    // hardware operations are critical
                    
                    retry++;
                    
                    try {
                    
                        Thread.sleep(1000);
                    
                    } catch (InterruptedException ex) {
                        
                        logger.error("Interrupted, ignored", ex);
                    }

                } finally {
                    NDC.pop();
                    NDC.remove();
                }
            }
        }

        protected abstract void execute() throws IOException;
    }
    
    private class CommandChangeMode extends Command {

        private final HvacMode mode;
        
        public CommandChangeMode(HvacDriver target, HvacMode mode) {
            super(target);
            
            this.mode = mode;
        }

        @Override
        protected void execute() throws IOException {
            
            target.setMode(mode);
        }
        
        @Override
        public String toString() {

            return "setMode(" + mode + ")";
        }
    }

    private class CommandSetRunning extends Command {

        private final boolean running;
        
        public CommandSetRunning(HvacDriver target, boolean running) {
            super(target);
            
            this.running = running;
        }

        @Override
        protected void execute() throws IOException {
            
            // VT: FIXME: Simplified
            
            target.setStage(running ? 1 : 0);
            target.setFanSpeed(running ? 1.0 : 0.0);
        }

        @Override
        public String toString() {

            return "setRunning(" + running + ")";
        }
    }

    private class CommandSetDemand extends Command {

        private final double demand;
        
        public CommandSetDemand(HvacDriver target, double demand) {
            super(target);
            
            this.demand = demand;
        }

        @Override
        protected void execute() throws IOException {

            logger.debug("ignored: setDemand(" + demand + ")");
        }

        @Override
        public String toString() {

            return "setDemand(" + demand + ")";
        }
    }
}
