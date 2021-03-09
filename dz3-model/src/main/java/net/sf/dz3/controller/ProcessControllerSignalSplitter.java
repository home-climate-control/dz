package net.sf.dz3.controller;

import org.apache.logging.log4j.ThreadContext;

import net.sf.dz3.util.digest.MessageDigestCache;
import net.sf.jukebox.datastream.logger.impl.DataBroadcaster;
import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.datastream.signal.model.DataSink;
import net.sf.jukebox.datastream.signal.model.DataSource;

public class ProcessControllerSignalSplitter implements DataSink<ProcessControllerStatus>, DataSource<Double> {

    private final DataBroadcaster<Double> dataBroadcaster = new DataBroadcaster<Double>();

    /**
     * Create an instance not attached to anything.
     */
    public ProcessControllerSignalSplitter() {
        
    }
    
    /**
     * Create an instance and add it as a listener to the given source.
     * 
     * @param controller Controller to listen to.
     */
    public ProcessControllerSignalSplitter(ProcessController source) {
        
        source.addConsumer(this);
    }

    @Override
    public void consume(DataSample<ProcessControllerStatus> signal) {
        
        ThreadContext.push("consume");
        
        try {
            
            long timestamp = signal.timestamp;
            String sourceName = signal.sourceName;
            
            consume(timestamp, sourceName + ".setpoint", signal.sample.setpoint);
            consume(timestamp, sourceName + ".error", signal.sample.error);
            consumeSignal(signal.sample.signal);
            
        } finally {
            ThreadContext.pop();
        }
    }
    
    /**
     * Consume an individual component of a process controller state.
     * 
     * @param timestamp Timestamp to consume with.
     * @param sourceName Source name to use as a base.
     * @param signal Signal to consume.
     */
    protected final void consume(long timestamp, String sourceName, double signal) {
        
        String signature = MessageDigestCache.getMD5(sourceName).substring(0, 19);
        DataSample<Double> output = new DataSample<Double>(timestamp, sourceName, signature, signal, null);

        dataBroadcaster.broadcast(output);
    }

    /**
     * Consume the process controller signal.
     * 
     * @param signal The process controller signal.
     */
    private void consumeSignal(DataSample<Double> signal) {
        
        String name = signal.sourceName + ".signal";
        String signature = MessageDigestCache.getMD5(name).substring(0, 19);
        DataSample<Double> output = new DataSample<Double>(signal.timestamp, name, signature, signal.sample, null);

        dataBroadcaster.broadcast(output);
    }

    @Override
    public final void addConsumer(DataSink<Double> consumer) {
        
        dataBroadcaster.addConsumer(consumer);
    }
    
    @Override
    public final void removeConsumer(DataSink<Double> consumer) {
        
        dataBroadcaster.removeConsumer(consumer);
    }
}
