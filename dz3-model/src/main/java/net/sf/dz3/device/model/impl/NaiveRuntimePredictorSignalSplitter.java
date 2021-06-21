package net.sf.dz3.device.model.impl;

import com.homeclimatecontrol.jukebox.datastream.logger.impl.DataBroadcaster;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSource;
import net.sf.dz3.device.model.UnitRuntimePredictionSignal;
import net.sf.dz3.util.digest.MessageDigestCache;

public class NaiveRuntimePredictorSignalSplitter implements DataSink<UnitRuntimePredictionSignal>, DataSource<Double> {
    private final DataBroadcaster<Double> dataBroadcaster = new DataBroadcaster<>();

    @Override
    public void consume(DataSample<UnitRuntimePredictionSignal> signal) {

        broadcastK(signal);
        broadcastLeft(signal);
    }

    private void broadcastK(DataSample<UnitRuntimePredictionSignal> signal) {
        var sourceName = signal.sourceName + ".k";
        var signature = MessageDigestCache.getMD5(sourceName).substring(0, 19);
        var k = new DataSample<Double>(signal.timestamp, sourceName, signature, signal.sample.k, null);
        dataBroadcaster.broadcast(k);
    }

    private void broadcastLeft(DataSample<UnitRuntimePredictionSignal> signal) {
        var sourceName = signal.sourceName + ".left";
        var signature = MessageDigestCache.getMD5(sourceName).substring(0, 19);
        var left = new DataSample<Double>(signal.timestamp, sourceName, signature, (double)signal.sample.left.toMillis(), null);
        dataBroadcaster.broadcast(left);
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
