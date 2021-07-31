package net.sf.dz3r.signal;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import net.sf.dz3.device.model.UnitSignal;
import net.sf.dz3.util.digest.MessageDigestCache;
import reactor.core.publisher.Flux;

/**
 * Consumes a complex {@link UnitSignal}, emits several
 * {@link DataSample simpler signals}
 * suitable for consumption by non-specialized metrics loggers.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2009-2021
 */
public class UnitSignalSplitter<T extends UnitSignal> {

    public Flux<DataSample<?>> split(Flux<DataSample<T>> source) {
        return source.flatMap(s -> Flux.just(getRunning(s), getDemand(s), getUptime(s)));
    }

    protected DataSample<Boolean> getRunning(DataSample<T> signal) {
        var sourceName = signal.sourceName + ".running";
        var signature = MessageDigestCache.getMD5(sourceName).substring(0, 19);
        return new DataSample<>(
                signal.timestamp, sourceName, signature,
                signal.isError() ? null : signal.sample.running,
                signal.isError() ? signal.error : null);
    }

    protected DataSample<Double> getDemand(DataSample<T> signal) {
        var sourceName = signal.sourceName + ".demand";
        var signature = MessageDigestCache.getMD5(sourceName).substring(0, 19);
        return new DataSample<>(
                signal.timestamp, sourceName, signature,
                signal.isError() ? null : signal.sample.demand,
                signal.isError() ? signal.error : null);
    }

    protected DataSample<Long> getUptime(DataSample<T> signal) {
        var sourceName = signal.sourceName + ".uptime";
        var signature = MessageDigestCache.getMD5(sourceName).substring(0, 19);
        return new DataSample<>(
                signal.timestamp, sourceName, signature,
                signal.isError() ? null : signal.sample.uptime,
                signal.isError() ? signal.error : null);
    }
}
