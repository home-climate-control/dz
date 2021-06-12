package net.sf.dz4.signal;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import net.sf.dz3.device.model.HvacMode;
import net.sf.dz3.device.model.HvacSignal;
import net.sf.dz3.util.digest.MessageDigestCache;
import reactor.core.publisher.Flux;

/**
 * Consumes a complex {@link net.sf.dz3.device.model.HvacSignal}, emits several
 * {@link com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample simpler signals}
 * suitable for consumption by non-specialized metrics loggers.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2009-2021
 */
public class HvacSignalSplitter {

    public Flux<DataSample<?>> split(Flux<DataSample<HvacSignal>> source) {
        return source.flatMap(s -> Flux.just(getMode(s), getRunning(s), getDemand(s), getUptime(s)));
    }

    private DataSample<HvacMode> getMode(DataSample<HvacSignal> signal) {
        var sourceName = signal.sourceName + ".mode";
        var signature = MessageDigestCache.getMD5(sourceName).substring(0, 19);
        return new DataSample<>(
                signal.timestamp, sourceName, signature,
                signal.isError() ? null : signal.sample.mode,
                signal.isError() ? signal.error : null);
    }

    private DataSample<Boolean> getRunning(DataSample<HvacSignal> signal) {
        var sourceName = signal.sourceName + ".running";
        var signature = MessageDigestCache.getMD5(sourceName).substring(0, 19);
        return new DataSample<>(
                signal.timestamp, sourceName, signature,
                signal.isError() ? null : signal.sample.running,
                signal.isError() ? signal.error : null);
    }

    private DataSample<Double> getDemand(DataSample<HvacSignal> signal) {
        var sourceName = signal.sourceName + ".demand";
        var signature = MessageDigestCache.getMD5(sourceName).substring(0, 19);
        return new DataSample<>(
                signal.timestamp, sourceName, signature,
                signal.isError() ? null : signal.sample.demand,
                signal.isError() ? signal.error : null);
    }

    private DataSample<Long> getUptime(DataSample<HvacSignal> signal) {
        var sourceName = signal.sourceName + ".uptime";
        var signature = MessageDigestCache.getMD5(sourceName).substring(0, 19);
        return new DataSample<>(
                signal.timestamp, sourceName, signature,
                signal.isError() ? null : signal.sample.uptime,
                signal.isError() ? signal.error : null);
    }
}
