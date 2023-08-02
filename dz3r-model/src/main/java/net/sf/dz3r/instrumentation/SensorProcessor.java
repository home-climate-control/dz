package net.sf.dz3r.instrumentation;

import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.SignalProcessor;
import net.sf.dz3r.signal.health.SensorStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.Flux;

import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Consumes individual sensor signal, emits sensor status.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2023
 */
public class SensorProcessor implements SignalProcessor<Double, SensorStatus, Void> {

    private final Logger logger = LogManager.getLogger();
    private final SortedSet<Double> diffs = new TreeSet<>();

    /**
     * Last known non-error signal value.
     */
    private Double lastKnown = null;

    /**
     * Calculated resolution. Is not affected by error signals, but may need to be adjusted for time windows at some point.
     */
    private Double resolution = null;

    @Override
    public Flux<Signal<SensorStatus, Void>> compute(Flux<Signal<Double, Void>> in) {
        return in.map(this::compute);
    }

    private Signal<SensorStatus, Void> compute(Signal<Double, Void> source) {

        if (source.isError()) {
            // Nothing else matters
            lastKnown = null;
            return new Signal<>(source.timestamp, null, null, source.status, source.error);
        }

        // VT: FIXME: Calculate signal stats

        return new Signal<>(
                source.timestamp,
                new SensorStatus(
                        Optional.ofNullable(source.getValue())
                                .map(this::computeResolution)
                                .orElse(this.resolution),
                        Optional.empty()));
    }

    private Double computeResolution(Double value) {

        ThreadContext.push("computeResolution");

        try {

            if (lastKnown != null) {

                // Stored as a set and not as a value to possibly improve the algorithm in the future,
                // including detecting noisy analog signals (might want to return NaN instead of null for that case)
                diffs.add(Math.abs(value - lastKnown));

                if (diffs.size() > 50) {
                    logger.warn("Noisy signal? Trimming the tail: {}", diffs);
                    ((TreeSet<?>) diffs).descendingIterator().remove();
                }
            }

            logger.debug("diffs: {}", diffs);

            lastKnown = value;
            resolution = computeResolution(diffs);

            return resolution;

        } finally {
            ThreadContext.pop();
        }
    }

    private Double computeResolution(SortedSet<Double> source) {

        if (source.size() < 2) {
            return null;
        }

        // Naïve version, assumes there will be the smallest difference, doesn't account for quickly changing signals,
        // nor does it account for noisy signals (except trimming the set to a reasonable length)

        return source.first();
    }
}
