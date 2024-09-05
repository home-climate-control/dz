package net.sf.dz3r.view.swing.zone;

import net.sf.dz3r.common.DataSet;
import com.homeclimatecontrol.hcc.signal.Signal;
import net.sf.dz3r.view.swing.AbstractChart;
import net.sf.dz3r.view.swing.ColorScheme;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
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
    protected static final Color ECO_COLOR_LOW = ColorScheme.coolingMap.setpoint.darker();
    protected static final Color ECO_COLOR_HIGH = ColorScheme.heatingMap.setpoint.darker();
    protected static final Color TARGET_COLOR = Color.GREEN.darker();

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

        // result.minmaxTime is null if the chart was never rendered since the start of the console
        logger.info("New minmaxTime set to + {}", () -> Optional.ofNullable(result.minmaxTime).map(t -> Duration.ofMillis(clock.instant().toEpochMilli() - t)).orElse(null));

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

            logger.trace("RingBuffer: flushing {} elements at {}", () -> count, () -> Duration.ofMillis(age));

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

            ambientAccumulator += value.economizerStatus.ambient().getValue();
            signalAccumulator += value.economizerStatus.callingStatus().sample();
        }

        @Override
        protected EconomizerTintedValue complete(ZoneChartDataPoint value, int count) {

            var result = new EconomizerTintedValue(ambientAccumulator / count, signalAccumulator / count);

            ambientAccumulator = value.economizerStatus.ambient().getValue();
            signalAccumulator = value.economizerStatus.callingStatus().sample();

            return result;
        }
    }
}
