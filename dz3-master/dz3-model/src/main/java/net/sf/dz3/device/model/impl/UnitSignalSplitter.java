package net.sf.dz3.device.model.impl;

import org.apache.logging.log4j.ThreadContext;

import net.sf.dz3.device.model.UnitSignal;
import net.sf.dz3.util.digest.MessageDigestCache;
import net.sf.jukebox.datastream.logger.impl.DataBroadcaster;
import net.sf.jukebox.datastream.logger.model.DataLogger;
import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.datastream.signal.model.DataSink;
import net.sf.jukebox.datastream.signal.model.DataSource;

/**
 * Receives a complex {@link UnitSignal} signal and converts it into several simpler
 * {@link DataSample} signals suitable for consumption by {@link DataLogger}.
 *
 * Add this object as a listener to the unit, and add the data logger as a listener to this object,
 * to record the data stream.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2009-2018
 */
public class UnitSignalSplitter implements DataSink<UnitSignal>, DataSource<Double> {

    private final DataBroadcaster<Double> dataBroadcaster = new DataBroadcaster<Double>();

    /**
     * Create an instance not attached to anything.
     */
    public UnitSignalSplitter() {

    }

    /**
     * Create an instance attached to a data source.
     *
     * @param source Data source to listen to.
     */
    public UnitSignalSplitter(DataSource<UnitSignal> source) {
        source.addConsumer(this);
    }

    @Override
    public synchronized void consume(DataSample<UnitSignal> signal) {

        ThreadContext.push("consume");

        try {

            {
                // Whether the unit is currently running
                String sourceName = signal.sourceName + ".running";
                String signature = MessageDigestCache.getMD5(sourceName).substring(0, 19);
                DataSample<Double> running = new DataSample<Double>(signal.timestamp, sourceName, signature, signal.sample.running ? 1.0 : 0.0, null);
                dataBroadcaster.broadcast(running);
            }

            {
                // The demand sent to the HVAC hardware driver
                String sourceName = signal.sourceName + ".demand";
                String signature = MessageDigestCache.getMD5(sourceName).substring(0, 19);
                DataSample<Double> running = new DataSample<Double>(signal.timestamp, sourceName, signature, signal.sample.demand, null);
                dataBroadcaster.broadcast(running);
            }

            {
                // Uptime recorded for instrumentation purposes
                String sourceName = signal.sourceName + ".uptime";
                String signature = MessageDigestCache.getMD5(sourceName).substring(0, 19);
                DataSample<Double> running = new DataSample<Double>(signal.timestamp, sourceName, signature, (double)signal.sample.uptime, null);
                dataBroadcaster.broadcast(running);
            }

        } finally {
            ThreadContext.pop();
        }
    }

    @Override
    public void addConsumer(DataSink<Double> consumer) {

        dataBroadcaster.addConsumer(consumer);
    }

    @Override
    public void removeConsumer(DataSink<Double> consumer) {

        dataBroadcaster.removeConsumer(consumer);
    }

}
