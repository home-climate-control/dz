package net.sf.dz3.device.model.impl;

import com.homeclimatecontrol.jukebox.datastream.logger.impl.DataBroadcaster;
import com.homeclimatecontrol.jukebox.datastream.logger.model.DataLogger;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSource;
import net.sf.dz3.device.model.HvacSignal;
import net.sf.dz3.util.digest.MessageDigestCache;
import org.apache.logging.log4j.ThreadContext;

/**
 * Receives a complex {@link HvacSignal} signal and converts it into several simpler
 * {@link DataSample} signals suitable for consumption by {@link DataLogger}.
 *
 * Add this object as a listener to the unit, and add the data logger as a listener to this object,
 * to record the data stream.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2009-2021
 */
public class HvacSignalSplitter implements DataSink<HvacSignal>, DataSource<Double> {

    private final DataBroadcaster<Double> dataBroadcaster = new DataBroadcaster<>();

    /**
     * Create an instance not attached to anything.
     */
    public HvacSignalSplitter() {

    }

    /**
     * Create an instance attached to a data source.
     *
     * @param source Data source to listen to.
     */
    public HvacSignalSplitter(DataSource<HvacSignal> source) {
        source.addConsumer(this);
    }

    @Override
    public synchronized void consume(DataSample<HvacSignal> signal) {

        ThreadContext.push("consume");

        try {

            broadcastMode(signal);
            broadcastRunning(signal);
            broadcastUptime(signal);
            broadcastDemand(signal);

        } finally {
            ThreadContext.pop();
        }
    }

    private void broadcastMode(DataSample<HvacSignal> signal) {
        // Current operating mode
        var sourceName = signal.sourceName + ".mode";
        var signature = MessageDigestCache.getMD5(sourceName).substring(0, 19);
        var mode = new DataSample<>(signal.timestamp, sourceName, signature, (double)signal.sample.mode.mode, null);
        dataBroadcaster.broadcast(mode);
    }

    private void broadcastRunning(DataSample<HvacSignal> signal) {
        // Whether the unit is currently running
        var sourceName = signal.sourceName + ".running";
        var signature = MessageDigestCache.getMD5(sourceName).substring(0, 19);
        var running = new DataSample<>(signal.timestamp, sourceName, signature, signal.sample.running ? 1.0 : 0.0, null);
        dataBroadcaster.broadcast(running);
    }

    private void broadcastUptime(DataSample<HvacSignal> signal) {
        // For how long the unit is currently running
        var sourceName = signal.sourceName + ".uptime";
        var signature = MessageDigestCache.getMD5(sourceName).substring(0, 19);
        var running = new DataSample<>(signal.timestamp, sourceName, signature, (double) signal.sample.uptime, null);
        dataBroadcaster.broadcast(running);
    }

    private void broadcastDemand(DataSample<HvacSignal> signal) {
        // The demand sent to the HVAC hardware driver
        var sourceName = signal.sourceName + ".demand";
        var signature = MessageDigestCache.getMD5(sourceName).substring(0, 19);
        var running = new DataSample<>(signal.timestamp, sourceName, signature, signal.sample.demand, null);
        dataBroadcaster.broadcast(running);
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
