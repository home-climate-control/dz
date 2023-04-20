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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Base class for zone chart implementations.
 *
 * Currently just {@link ZoneChart2021}, all others lost the evolutionary performance battle.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public abstract class AbstractZoneChart extends AbstractChart<ZoneChartDataPoint, Void> {

    /**
     * Thermostat output signals.
     */
    protected final transient DataSet<ThermostatTintedValue> dsValues = new DataSet<>(chartLengthMillis);

    /**
     * Thermostat setpoints.
     */
    protected final transient DataSet<Double> dsSetpoints = new DataSet<>(chartLengthMillis);

    /**
     * Economizer status signals.
     */
    protected final transient DataSet<EconomizerTintedValue> dsEconomizer = new DataSet<>(chartLengthMillis);

    /**
     * Economizer target temperatures.
     */
    protected final transient DataSet<Double> dsTargets = new DataSet<>(chartLengthMillis);

    /**
     * Lock common for all the data sets. Suboptimal, but not a bottleneck.
     */
    protected final transient ReadWriteLock lock = new ReentrantReadWriteLock();

    protected static final Color SIGNAL_COLOR_LOW = Color.GREEN;
    protected static final Color SIGNAL_COLOR_HIGH = Color.RED;
    protected static final Color ECO_COLOR_LOW = new Color(0x0C, 0xC3, 0xFA);
    protected static final Color ECO_COLOR_HIGH = new Color(0xFF, 0xBC, 0x1C);
    protected static final Color TARGET_COLOR = Color.GREEN;

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
        paintChart(g2d, boundary, insets, now, xScale, xOffset, yScale, yOffset, dsValues, dsEconomizer, lock, dsTargets, dsSetpoints);
    }

    @SuppressWarnings("squid:S107")
    protected abstract void paintChart(
            Graphics2D g2d, Dimension boundary, Insets insets, long now,
            double xScale, long xOffset, double yScale, double yOffset,
            DataSet<ThermostatTintedValue> dsValues,
            DataSet<EconomizerTintedValue> dsEconomizer,
            ReadWriteLock lock,
            DataSet<Double> dsTargets,
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

        if (signal < -1.0 || signal > 1.0) {
            throw new IllegalArgumentException("signal (" + signal + ") is outside of -1...1 range ");
        }

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

        for (var i = dsValues.entryIterator(); i.hasNext(); ) {

            var entry = i.next();
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

        logger.info("minmax/thermostat set to {}/{}", min, max);

        for (var i = dsEconomizer.entryIterator(); i.hasNext(); ) {

            var entry = i.next();
            var timestamp = entry.getKey();
            var tv = entry.getValue();

            if (max == null || tv.ambient > max) {
                max = tv.ambient;
                minmaxTime = timestamp;
            }

            if (min == null || tv.ambient < min) {
                min = tv.ambient;
                minmaxTime = timestamp;
            }
        }

        logger.info("minmax/eco adjusted to   {}/{}", min, max);

        var result = new Limits(min, max, minmaxTime);

        logger.info("Recalculated in {}ms", (clock.instant().toEpochMilli() - startTime));
        logger.info("New minmaxTime set to + {}", () -> Duration.ofMillis(clock.instant().toEpochMilli() - result.minmaxTime));

        return result;
    }

    /**
     * Averaging tool.
     */
    protected abstract class Averager<I, O> {

        /**
         * The expiration interval. Values older than the last key by this many
         * milliseconds are expired.
         */
        private final long expirationInterval;

        /**
         * The timestamp of the oldest recorded sample.
         */
        protected Long oldestTimestamp;

        private int count;

        protected Averager(long expirationInterval) {
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
        public final O append(Signal<I, Void> signal) {

            if (oldestTimestamp == null) {
                oldestTimestamp = signal.timestamp.toEpochMilli();
            }

            var age = signal.timestamp.toEpochMilli() - oldestTimestamp;

            if (age < expirationInterval) {

                count++;

                accumulate(signal.getValue());

                return null;
            }

            logger.trace("RingBuffer: flushing at {}", () -> Duration.ofMillis(age));

            var result = complete(signal.getValue(), count);

            count = 1;
            oldestTimestamp = signal.timestamp.toEpochMilli();

            return result;
        }

        protected abstract void accumulate(I value);
        protected abstract O complete(I value, int count);
    }

    protected class ThermostatAverager extends Averager<ZoneChartDataPoint, ThermostatTintedValue> {

        private double valueAccumulator = 0;
        private double tintAccumulator = 0;
        private double emphasizeAccumulator = 0;

        public ThermostatAverager(long expirationInterval) {
            super(expirationInterval);
        }

        @Override
        protected void accumulate(ZoneChartDataPoint value) {

            valueAccumulator += value.tintedValue.value;
            tintAccumulator += value.tintedValue.tint;
            emphasizeAccumulator += value.tintedValue.emphasize ? 1.0 : 0.0;
        }

        @Override
        protected ThermostatTintedValue complete(ZoneChartDataPoint value, int count) {

            var result = new ThermostatTintedValue(valueAccumulator / count, tintAccumulator / count, emphasizeAccumulator > 0);

            valueAccumulator = value.tintedValue.value;
            tintAccumulator = value.tintedValue.tint;
            emphasizeAccumulator = 0;

            return result;
        }
    }

    protected class EconomizerAverager extends Averager<ZoneChartDataPoint, EconomizerTintedValue> {

        private double ambientAccumulator = 0;
        private double signalAccumulator = 0;

        public EconomizerAverager(long expirationInterval) {
            super(expirationInterval);
        }

        @Override
        protected void accumulate(ZoneChartDataPoint value) {

            ambientAccumulator += value.economizerStatus.ambient.getValue();
            signalAccumulator += value.economizerStatus.callingStatus.sample;
        }

        @Override
        protected EconomizerTintedValue complete(ZoneChartDataPoint value, int count) {

            var result = new EconomizerTintedValue(ambientAccumulator / count, signalAccumulator / count);

            ambientAccumulator = value.economizerStatus.ambient.getValue();
            signalAccumulator = value.economizerStatus.callingStatus.sample;

            return result;
        }
    }
}
