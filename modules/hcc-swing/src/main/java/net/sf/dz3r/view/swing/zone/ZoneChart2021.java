package net.sf.dz3r.view.swing.zone;

import com.homeclimatecontrol.hcc.model.EconomizerSettings;
import net.sf.dz3r.common.DataSet;
import net.sf.dz3r.signal.Signal;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;

public class ZoneChart2021 extends AbstractZoneChart {

    private transient ThermostatAverager thermostatAverager;
    private transient EconomizerAverager economizerAverager;

    /**
     * Thermostat signal to color cache.
     */
    private final transient SignalColorCache thermostatSignalCache = new SignalColorCache(SIGNAL_COLOR_LOW, SIGNAL_COLOR_HIGH);

    /**
     * Economizer signal to color cache. Note that low and high colors are reversed due to economizer logic
     */
    private final transient SignalColorCache economizerSignalCache = new SignalColorCache(ECO_COLOR_HIGH, ECO_COLOR_LOW);

    private static final int ECO_ALPHA = 0x80;

    protected ZoneChart2021(Clock clock, long chartLengthMillis, boolean needFahrenheit) {
        super(clock, chartLengthMillis, needFahrenheit);
    }

    @Override
    protected boolean update(Signal<ZoneChartDataPoint, Void> signal) {

        // Economizer signal may be unavailable, either yet, or at all

        Double ambient;
        Double target;

        if (signal.getValue().economizerStatus == null) {

            logger.trace("eco: null");

            ambient = null;
            target = null;

        } else {

            logger.trace("eco: {}", signal.getValue().economizerStatus);

            ambient = Optional.ofNullable(signal.getValue().economizerStatus.ambient()).map(Signal::getValue).orElse(null);
            target = Optional.ofNullable(signal.getValue().economizerStatus.settings()).map(EconomizerSettings::targetTemperature).orElse(null);
        }

        logger.trace("ambient={}, target={}", ambient, target);

        adjustVerticalLimits(
                signal.timestamp.toEpochMilli(),
                signal.getValue().tintedValue.value,
                signal.getValue().setpoint,
                ambient,
                target);

        synchronized (AbstractZoneChart.class) {

            if (localWidth != getGlobalWidth()) {

                // Chart size changed, need to adjust the buffer
                localWidth = getGlobalWidth();

                var step = chartLengthMillis / localWidth;

                logger.info("new width {}, {}ms per pixel", localWidth, step);

                // We lose one sample this way, might want to improve it later, for now, no big deal
                thermostatAverager = new ThermostatAverager(step);
                economizerAverager = new EconomizerAverager(step);

                return true;
            }
        }

        if (localWidth == 0) {

            // There's nothing we can do before the width is set.
            // It's not even worth it to record the value.

            // VT: NOTE: This used to be a pretty often encountered race condition. Nowadays, it's a sign of a programming error.
            // VT: NOTE: ...or a design change. With arrival of InstrumentCluster, this panel may not see the light of day
            // for a long time. For now, down to DEBUG level, will figure out a better solution... in another decade or so.

            logger.debug("please repaint (is everything all right? did we leave the dashboard yet?)");
            return true;
        }

        // VT: NOTE: Imperative code sucks here, it would be nice to rewrite the averagers as reactive
        // and flatmap the hell out of null values

        // These two may be non-null at different times, must analyze them separately
        var thermostatTintedValue = thermostatAverager.append(signal);
        var economizerTintedValue = (signal.getValue().economizerStatus == null || signal.getValue().economizerStatus.ambient() == null)
                ? null
                : economizerAverager.append(signal);

        logger.trace("thermostatTintedValue={}", thermostatTintedValue);
        logger.trace("economizerTintedValue={}", economizerTintedValue);

        var timestamp = signal.timestamp.toEpochMilli();

        // VT: NOTE: Write lock is acquired once per all sets, it's a short operation
        var lockNow = Instant.now().toEpochMilli();
        lock.writeLock().lock();

        try {

            logger.debug("write lock acquired in {}ms", Instant.now().toEpochMilli() - lockNow);

            var thermostatChange = capture(timestamp, thermostatTintedValue, signal.getValue().setpoint);
            var economizerChange = capture(timestamp, economizerTintedValue, target);

            return thermostatChange || economizerChange;

        } finally {
            lock.writeLock().unlock();
        }
    }

    private boolean capture(long timestamp, ThermostatTintedValue thermostatTintedValue, double setpoint) {

        if (thermostatTintedValue == null) {
            // The average is still being calculated, nothing to do
            return false;
        }

        dsValues.append(timestamp, thermostatTintedValue, true);
        dsSetpoints.append(timestamp, setpoint, true);

        return true;
    }

    private boolean capture(long timestamp, EconomizerTintedValue economizerTintedValue, Double target) {

        if (target == null) {
            // There is no economizer in this zone
            return false;
        }

        if (economizerTintedValue == null) {
            // The average is still being calculated, nothing to do
            return false;
        }

        dsEconomizer.append(timestamp, economizerTintedValue, true);
        dsTargets.append(timestamp, target, true);

        return true;
    }

    @Override
    protected void paintChart(Graphics2D g2d, Dimension boundary, Insets insets,
                              long now, double xScale, long xOffset, double yScale, double yOffset,
                              DataSet<ThermostatTintedValue> dsValues, DataSet<EconomizerTintedValue> dsEconomizer, ReadWriteLock lock,
                              DataSet<Double> dsTargets, DataSet<Double> dsSetpoints) {

        // Layer order: economizer, thermostat, economizer target, setpoint

        paintEconomizerValues(g2d, insets, now, xScale, xOffset, yScale, yOffset, dsEconomizer, lock);
        paintThermostatValues(g2d, insets, now, xScale, xOffset, yScale, yOffset, dsValues, lock);

        paintTargets(g2d, insets, xScale, xOffset, yScale, yOffset, dsTargets);
        paintSetpoints(g2d, insets, xScale, xOffset, yScale, yOffset, dsSetpoints);
    }

    @SuppressWarnings("squid:S107")
    private void paintEconomizerValues(Graphics2D g2d, Insets insets,
                                       long now, double xScale, long xOffset, double yScale, double yOffset,
                                       DataSet<EconomizerTintedValue> ds, ReadWriteLock lock) {

        var lockNow = Instant.now().toEpochMilli();

        lock.readLock().lock();
        try {

            logger.debug("read/eco lock acquired in {}ms", Instant.now().toEpochMilli() - lockNow);

            Long timeTrailer = null;
            EconomizerTintedValue trailer = null;

            for (var di = ds.entryIterator(); di.hasNext(); ) {

                var entry = di.next();
                var timeNow = entry.getKey();
                var cursor = entry.getValue();

                if (timeTrailer != null) {

                    var x0 = (timeTrailer - xOffset) * xScale + insets.left;
                    var y0 = (yOffset - trailer.ambient) * yScale + insets.top;

                    var x1 = (timeNow - xOffset) * xScale + insets.left;
                    var y1 = (yOffset - cursor.ambient) * yScale + insets.top;

                    // Decide whether the line is alive or dead

                    if (timeNow - timeTrailer > DEAD_TIMEOUT.toMillis()) {

                        // It's dead, all right
                        // Paint the horizontal line in dead color and skew the x0 so the next part will be painted vertical
                        var startColor = economizerSignalCache.signal2color(trailer.signal - 1, ECO_ALPHA);

                        // End color differs from the start in alpha, not hue - this plays nicer with backgrounds
                        // Even though this is a memory allocation, it won't affect performance since [hopefully]
                        // there'll be just a few dead drops
                        var endColor = new Color(startColor.getRed(), startColor.getGreen(), startColor.getBlue(), 0x40);

                        drawGradientLine(g2d, x0, y0, x1, y0, startColor, endColor, false);

                        x0 = x1;
                    }

                    var startColor = economizerSignalCache.signal2color(trailer.signal - 1, ECO_ALPHA);
                    var endColor = economizerSignalCache.signal2color(cursor.signal - 1, ECO_ALPHA);

                    drawGradientLine(g2d, x0, y0, x1, y1, startColor, endColor, false);
                }

                timeTrailer = timeNow;
                trailer = cursor;
            }

            if (timeTrailer != null && now - timeTrailer > DEAD_TIMEOUT.toMillis()) {

                // There's a gap on the right, let's fill it

                var x0 = (timeTrailer - xOffset) * xScale + insets.left;
                var x1 = (now - xOffset) * xScale + insets.left;
                var y = (yOffset - trailer.ambient) * yScale + insets.top;

                var startColor = economizerSignalCache.signal2color(trailer.signal - 1, ECO_ALPHA);
                var endColor = getBackground();

                drawGradientLine(g2d, x0, y, x1, y, startColor, endColor, false);
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    private void paintThermostatValues(Graphics2D g2d, Insets insets,
                                       long now, double xScale, long xOffset, double yScale, double yOffset,
                                       DataSet<ThermostatTintedValue> ds, ReadWriteLock lock) {

        var lockNow = Instant.now().toEpochMilli();

        lock.readLock().lock();
        try {

            logger.debug("read/values lock acquired in {}ms", Instant.now().toEpochMilli() - lockNow);

            Long timeTrailer = null;
            ThermostatTintedValue trailer = null;

            for (var di = ds.entryIterator(); di.hasNext(); ) {

                var entry = di.next();
                var timeNow = entry.getKey();
                var cursor = entry.getValue();

                if (timeTrailer != null) {

                    var x0 = (timeTrailer - xOffset) * xScale + insets.left;
                    var y0 = (yOffset - trailer.value) * yScale + insets.top;

                    var x1 = (timeNow - xOffset) * xScale + insets.left;
                    var y1 = (yOffset - cursor.value) * yScale + insets.top;

                    // Decide whether the line is alive or dead

                    if (timeNow - timeTrailer > DEAD_TIMEOUT.toMillis()) {

                        // It's dead, all right
                        // Paint the horizontal line in dead color and skew the x0 so the next part will be painted vertical
                        var startColor = thermostatSignalCache.signal2color(trailer.tint - 1);

                        // End color differs from the start in alpha, not hue - this plays nicer with backgrounds
                        // Even though this is a memory allocation, it won't affect performance since [hopefully]
                        // there'll be just a few dead drops
                        var endColor = new Color(startColor.getRed(), startColor.getGreen(), startColor.getBlue(), 0x40);

                        drawGradientLine(g2d, x0, y0, x1, y0, startColor, endColor, cursor.emphasize);

                        x0 = x1;
                    }

                    var startColor = thermostatSignalCache.signal2color(trailer.tint - 1);
                    var endColor = thermostatSignalCache.signal2color(cursor.tint - 1);

                    drawGradientLine(g2d, x0, y0, x1, y1, startColor, endColor, cursor.emphasize);
                }

                timeTrailer = timeNow;
                trailer = cursor;
            }

            if (timeTrailer != null && now - timeTrailer > DEAD_TIMEOUT.toMillis()) {

                // There's a gap on the right, let's fill it

                var x0 = (timeTrailer - xOffset) * xScale + insets.left;
                var x1 = (now - xOffset) * xScale + insets.left;
                var y = (yOffset - trailer.value) * yScale + insets.top;

                var startColor = thermostatSignalCache.signal2color(trailer.tint - 1);
                var endColor = getBackground();

                drawGradientLine(g2d, x0, y, x1, y, startColor, endColor, false);
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    private void paintSetpoints(Graphics2D g2d, Insets insets,
                                double xScale, long xOffset, double yScale, double yOffset,
                                DataSet<Double> ds) {

        paintSetpointLines(g2d, insets, xScale, xOffset, yScale, yOffset, ds, SETPOINT_COLOR);
    }
    private void paintTargets(Graphics2D g2d, Insets insets,
                              double xScale, long xOffset, double yScale, double yOffset,
                              DataSet<Double> ds) {

        paintSetpointLines(g2d, insets, xScale, xOffset, yScale, yOffset, ds, TARGET_COLOR);
    }
    private void paintSetpointLines(Graphics2D g2d, Insets insets,
                                    double xScale, long xOffset, double yScale, double yOffset,
                                    DataSet<Double> ds,
                                    Color baseColor) {

        var startColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 64);

        Long timeTrailer = null;

        // VT: NOTE: This iterator is not protected by the read lock, the probability of concurrent access is much lower.
        // If it starts blowing up, though...

        for (var di = ds.entryIterator(); di.hasNext();) {

            var entry = di.next();
            var timeNow = entry.getKey();
            var cursor = entry.getValue();

            double x0;
            double x1;
            var y = (yOffset - cursor) * yScale + insets.top;

            if (timeTrailer == null) {
                x0 = insets.left;
                x1 = (timeNow - xOffset) * xScale + insets.left;

            } else {
                x0 = (timeTrailer - xOffset) * xScale + insets.left;
                x1 = (timeNow - xOffset) * xScale + insets.left;
            }

            drawGradientLine(g2d, x0, y, x1, y, startColor, baseColor, false);

            timeTrailer = timeNow;
        }
    }
}
