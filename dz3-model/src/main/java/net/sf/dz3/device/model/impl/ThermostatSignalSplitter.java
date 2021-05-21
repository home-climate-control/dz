package net.sf.dz3.device.model.impl;

import com.homeclimatecontrol.jukebox.datastream.logger.impl.DataBroadcaster;
import com.homeclimatecontrol.jukebox.datastream.logger.model.DataLogger;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSource;
import net.sf.dz3.device.model.Thermostat;
import net.sf.dz3.device.model.ThermostatSignal;
import net.sf.dz3.util.digest.MessageDigestCache;
import org.apache.logging.log4j.ThreadContext;

/**
 * Receives a complex {@link ThermostatSignal} signal and converts it into several simpler
 * {@link DataSample} signals suitable for consumption by {@link DataLogger}.
 *
 * Add this object as a listener to the thermostat, and add the data logger as a listener to this object,
 * to record the data stream.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2009-2018
 */
public class ThermostatSignalSplitter implements DataSink<ThermostatSignal>, DataSource<Double> {

    private final DataBroadcaster<Double> dataBroadcaster = new DataBroadcaster<Double>();

    /**
     * Create an instance not attached to anything.
     */
    public ThermostatSignalSplitter() {

    }

    /**
     * Create an instance attached to a thermostat.
     *
     * @param ts Thermostat to listen to.
     */
    public ThermostatSignalSplitter(Thermostat ts) {
        ts.addConsumer(this);
    }

    @Override
    public synchronized void consume(DataSample<ThermostatSignal> signal) {

        ThreadContext.push("consume");

        try {

            {
                // Whether this thermostat is enabled
                String sourceName = signal.sourceName + ".enabled";
                String signature = MessageDigestCache.getMD5(sourceName).substring(0, 19);
                DataSample<Double> calling = new DataSample<Double>(signal.timestamp, sourceName, signature, signal.sample.enabled ? 1.0 : 0.0, null);
                dataBroadcaster.broadcast(calling);
            }

            {
                // Whether this thermostat is on hold
                String sourceName = signal.sourceName + ".hold";
                String signature = MessageDigestCache.getMD5(sourceName).substring(0, 19);
                DataSample<Double> calling = new DataSample<Double>(signal.timestamp, sourceName, signature, signal.sample.onHold ? 1.0 : 0.0, null);
                dataBroadcaster.broadcast(calling);
            }

            {
                // Whether this thermostat is calling
                String sourceName = signal.sourceName + ".calling";
                String signature = MessageDigestCache.getMD5(sourceName).substring(0, 19);
                DataSample<Double> calling = new DataSample<Double>(signal.timestamp, sourceName, signature, signal.sample.calling ? 1.0 : 0.0, null);
                dataBroadcaster.broadcast(calling);
            }

            {
                // Whether this thermostat is voting
                String sourceName = signal.sourceName + ".voting";
                String signature = MessageDigestCache.getMD5(sourceName).substring(0, 19);
                DataSample<Double> calling = new DataSample<Double>(signal.timestamp, sourceName, signature, signal.sample.voting ? 1.0 : 0.0, null);
                dataBroadcaster.broadcast(calling);
            }

            // The demand sent to the zone controller
            dataBroadcaster.broadcast(signal.sample.demand);

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
