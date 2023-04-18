package net.sf.dz3r.view.swing.zone;

import net.sf.dz3r.common.DataSet;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.view.swing.AbstractChart;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.time.Clock;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Base class for zone chart implementations.
 *
 * Currently just {@link ZoneChart2021}, all others lost the evolutionary performance battle.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public abstract class AbstractZoneChart extends AbstractChart<TintedValueAndSetpoint, Void> {

    protected final transient DataSet<TintedValue> dsValues = new DataSet<>(chartLengthMillis);
    protected final transient DataSet<Double> dsSetpoints = new DataSet<>(chartLengthMillis);

    protected final transient ReadWriteLock lockValues = new ReentrantReadWriteLock();

    protected static final Color SIGNAL_COLOR_LOW = Color.GREEN;
    protected static final Color SIGNAL_COLOR_HIGH = Color.RED;
    protected static final Color SETPOINT_COLOR = Color.YELLOW;

    protected AbstractZoneChart(Clock clock, long chartLengthMillis, boolean needFahrenheit) {
        super(clock, chartLengthMillis, needFahrenheit);
    }

    @Override
    protected final boolean isDataAvailable() {
        return !dsValues.isEmpty() && dataMax != null && dataMin != null;
    }

    @Override
    protected final void paintCharts(
            Graphics2D g2d, Dimension boundary, Insets insets, long now,
            double xScale, long xOffset, double yScale, double yOffset) {

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        paintChart(g2d, boundary, insets, now, xScale, xOffset, yScale, yOffset, dsValues, lockValues, dsSetpoints);
    }

    @SuppressWarnings("squid:S107")
    protected abstract void paintChart(
            Graphics2D g2d, Dimension boundary, Insets insets, long now,
            double xScale, long xOffset, double yScale, double yOffset,
            DataSet<TintedValue> dsValues, ReadWriteLock lockValues,
            DataSet<Double> dsSetpoints);

    private static final Color[] signalCache = new Color[256];

    /**
     * Convert signal from -1 to +1 to color from low color to high color.
     *
     * @param signal Signal to convert to color.
     * @param low Color corresponding to -1 signal value.
     * @param high Color corresponding to +1 signal value.
     *
     * @return Color resolved from the incoming signal.
     */
    protected final Color signal2color(double signal, Color low, Color high) {

        signal = signal > 1 ? 1: signal;
        signal = signal < -1 ? -1 : signal;
        signal = (signal + 1) / 2;

        int index = (int) (signal * 255);

        synchronized (signalCache) {

            Color result = signalCache[index];

            if ( result == null) {

                float[] hsbLow = resolve(low);
                float[] hsbHigh = resolve(high);

                float h = transform(signal, hsbLow[0], hsbHigh[0]);
                float s = transform(signal, hsbLow[1], hsbHigh[1]);
                float b = transform(signal, hsbLow[2], hsbHigh[2]);

                result = new Color(Color.HSBtoRGB(h, s, b));
                signalCache[index] = result;
            }

            return result;
        }
    }

    private static class RGB2HSB {

        public final int rgb;
        public final float[] hsb;

        public RGB2HSB(int rgb, float[] hsb) {

            this.rgb = rgb;
            this.hsb = hsb;
        }
    }

    /**
     * Cache medium for {@link #resolve(Color)}.
     *
     * According to "worse is better" rule, there's no error checking against
     * the array size - too expensive. In all likelihood, this won't grow beyond 2 entries.
     */
    private static final RGB2HSB[] rgb2hsb = new RGB2HSB[16];

    /**
     * Resolve a possibly cached {@link Color#RGBtoHSB(int, int, int, float[])} result,
     * or compute it and store it for later retrieval if it hasn't been done.
     *
     * @param color Color to transform.
     * @return Transformation result.
     */
    private float[] resolve(Color color) {

        var rgb = color.getRGB();
        var offset = 0;

        for (; offset < rgb2hsb.length && rgb2hsb[offset] != null; offset++) {

            if (rgb == rgb2hsb[offset].rgb) {

                return rgb2hsb[offset].hsb;
            }
        }

        synchronized (rgb2hsb) {

            // VT: NOTE: Not the cleanest solution. It is possible that someone has just tried to do the same thing
            // and we'll end up writing the same value twice, but oh well, it's the same value in an array of size 16

            rgb2hsb[offset] = new RGB2HSB(rgb, Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null));
        }

        logger.info("RGB2HSB offset={}", offset );

        return rgb2hsb[offset].hsb;
    }

    /**
     * Get the point between the start and end values corresponding to the value of the signal.
     *
     * @param signal Signal value, from -1 to +1.
     * @param start Start point.
     * @param end End point.
     *
     * @return Desired position between the start and end points.
     */
    private float transform(double signal, float start, float end) {

        assert(signal <= 1);
        assert(signal >= -1);

        return (float) (start + signal * (end - start));
    }

    /**
     * Calculate {@link #dataMin} and {@link #dataMax} based on all values available in {@link #dsValues}.
     */
    @Override
    protected Limits recalculateVerticalLimits() {

        var startTime = clock.instant().toEpochMilli();

        Double min = null;
        Double max = null;
        Long minmaxTime = null;

        for (Iterator<Map.Entry<Long, TintedValue>> i2 = dsValues.entryIterator(); i2.hasNext(); ) {

            var entry = i2.next();
            var timestamp = entry.getKey();
            var tv = entry.getValue();

            if (max == null || tv.value > max) {
                max = tv.value;
                minmaxTime = timestamp;
            }

            if (min == null || tv.value < min) {
                min = tv.value;
                minmaxTime = timestamp;
            }
        }

        var result = new Limits(min, max, minmaxTime);

        logger.info("Recalculated in {}ms", (clock.instant().toEpochMilli() - startTime));
        logger.info("New minmaxTime set to + {}", () -> Duration.ofMillis(clock.instant().toEpochMilli() - result.minmaxTime));

        return result;
    }

    /**
     * Averaging tool.
     */
    protected class Averager {

        /**
         * The expiration interval. Values older than the last key by this many
         * milliseconds are expired.
         */
        private final long expirationInterval;

        /**
         * The timestamp of the oldest recorded sample.
         */
        private Long timestamp;

        private int count;
        private double valueAccumulator = 0;
        private double tintAccumulator = 0;
        private double emphasizeAccumulator = 0;

        public Averager(long expirationInterval) {
            this.expirationInterval = expirationInterval;
        }

        /**
         * Record a value.
         *
         * @param signal Signal to record.
         *
         * @return The average of all data stored in the buffer if this sample is more than {@link #expirationInterval}
         * away from the first sample stored, {@code null} otherwise.
         */
        public TintedValue append(Signal<? extends TintedValue, Void> signal) {

            if (timestamp == null) {
                timestamp = signal.timestamp.toEpochMilli();
            }

            var age = signal.timestamp.toEpochMilli() - timestamp;

            if ( age < expirationInterval) {

                count++;
                valueAccumulator += signal.getValue().value;
                tintAccumulator += signal.getValue().tint;
                emphasizeAccumulator += signal.getValue().emphasize ? 1 : 0;

                return null;
            }

            logger.debug("RingBuffer: flushing at {}", () -> Duration.ofMillis(age));

            var result = new TintedValue(valueAccumulator / count, tintAccumulator / count, emphasizeAccumulator > 0);

            count = 1;
            valueAccumulator = signal.getValue().value;
            tintAccumulator = signal.getValue().tint;
            emphasizeAccumulator = 0;
            timestamp = signal.timestamp.toEpochMilli();

            return result;
        }
    }
}
