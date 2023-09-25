package net.sf.dz3r.instrumentation;

import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.SignalProcessor;
import net.sf.dz3r.signal.health.SensorStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Consumes individual sensor signal, emits sensor status.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2023
 */
public class SensorStatusProcessor implements SignalProcessor<Double, SensorStatus, Void> {

    private final Logger logger = LogManager.getLogger();

    private final String id;
    private final SortedSet<Double> diffs = new TreeSet<>();

    /**
     * Last known non-error signal value.
     */
    private Double lastKnown = null;

    /**
     * Calculated resolution. Is not affected by error signals, but may need to be adjusted for time windows at some point.
     */
    private Double resolution = null;

    public SensorStatusProcessor(String id) {
        this.id = id;

        logger.info("created sensor status processor for id={}", id);
    }

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

        ThreadContext.push("computeResolution#" + id);

        try {

            if (lastKnown != null) {

                // Stored as a set and not as a value to possibly improve the algorithm in the future,
                // including detecting noisy analog signals (might want to return NaN instead of null for that case)

                var diff = round(Math.abs(value - lastKnown));

                if (Double.compare(diff, 0.0) == 0) {
                    // Sorry, no cigar
                    return resolution;
                }

                diffs.add(diff);

                if (diffs.size() > 50) {
                    logger.warn("Noisy signal? Trimming the tail: {}", diffs);

                    var i = ((TreeSet<?>) diffs).descendingIterator();
                    i.next();
                    i.remove();
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

    private double round(Double d) {
        return
                BigDecimal.valueOf(d)
                        .setScale(3, RoundingMode.HALF_UP)
                        .doubleValue();
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
