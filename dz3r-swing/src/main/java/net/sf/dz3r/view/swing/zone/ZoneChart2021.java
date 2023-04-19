package net.sf.dz3r.view.swing.zone;

import net.sf.dz3r.common.DataSet;
import net.sf.dz3r.signal.Signal;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.time.Clock;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

public class ZoneChart2021 extends AbstractZoneChart {

    private transient ThermostatAverager thermostatAverager;
    private transient EconomizerAverager economizerAverager;

    protected ZoneChart2021(Clock clock, long chartLengthMillis, boolean needFahrenheit) {
        super(clock, chartLengthMillis, needFahrenheit);
    }

    @Override
    protected void update() {

        if (append(getSignal())) {
            repaint();
        }
    }

    private boolean append(Signal<ZoneChartDataPoint, Void> signal) {

        adjustVerticalLimits(signal.timestamp.toEpochMilli(), signal.getValue().tintedValue.value, signal.getValue().setpoint);

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

            logger.info("please repaint");
            return true;
        }

        var thermostatTintedValue = thermostatAverager.append(signal);
        var economizerTintedValue = (signal.getValue().economizerStatus == null || signal.getValue().economizerStatus.ambient == null)
                ? null
                : economizerAverager.append(signal);

        if (thermostatTintedValue == null) {
            // The average is still being calculated, nothing to do
            return false;
        }

        var lockNow = Instant.now().toEpochMilli();
        lock.writeLock().lock();

        try {

            // VT: NOTE: Write lock is acquired once per all sets, it's a short operation

            logger.debug("write lock acquired in {}ms", Instant.now().toEpochMilli() - lockNow);

            var timestamp = signal.timestamp.toEpochMilli();

            dsValues.append(timestamp, thermostatTintedValue, true);
            dsSetpoints.append(timestamp, signal.getValue().setpoint, true);

            if (economizerTintedValue != null) {

                dsEconomizer.append(timestamp, economizerTintedValue, true);
                dsTargets.append(timestamp, signal.getValue().economizerStatus.settings.targetTemperature, true);
            }

        } finally {
            lock.writeLock().unlock();
        }


        return true;
    }

    @Override
    protected void paintChart(Graphics2D g2d, Dimension boundary, Insets insets,
                              long now, double xScale, long xOffset, double yScale, double yOffset,
                              DataSet<ThermostatTintedValue> dsValues, ReadWriteLock lock, DataSet<Double> dsTargets, DataSet<Double> dsSetpoints) {

        // Layer order: economizer, thermostat, economizer target, setpoint

        paintValues(g2d, insets, now, xScale, xOffset, yScale, yOffset, dsValues, lock);

        paintTargets(g2d, insets, xScale, xOffset, yScale, yOffset, dsTargets);
        paintSetpoints(g2d, insets, xScale, xOffset, yScale, yOffset, dsSetpoints);
    }

    @SuppressWarnings("squid:S107")
    private void paintValues(Graphics2D g2d, Insets insets,
                             long now, double xScale, long xOffset, double yScale, double yOffset,
                             DataSet<ThermostatTintedValue> ds, ReadWriteLock lock) {

        var lockNow = Instant.now().toEpochMilli();

        lock.readLock().lock();
        try {

            logger.debug("read/values lock acquired in {}ms", Instant.now().toEpochMilli() - lockNow);

            Long timeTrailer = null;
            ThermostatTintedValue trailer = null;

            for (Iterator<Map.Entry<Long, ThermostatTintedValue>> di = ds.entryIterator(); di.hasNext(); ) {

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
                        var startColor = signal2color(trailer.tint - 1, SIGNAL_COLOR_LOW, SIGNAL_COLOR_HIGH);

                        // End color differs from the start in alpha, not hue - this plays nicer with backgrounds
                        // Even though this is a memory allocation, it won't affect performance since [hopefully]
                        // there'll be just a few dead drops
                        var endColor = new Color(startColor.getRed(), startColor.getGreen(), startColor.getBlue(), 64);

                        drawGradientLine(g2d, x0, y0, x1, y0, startColor, endColor, cursor.emphasize);

                        x0 = x1;
                    }

                    var startColor = signal2color(trailer.tint - 1, SIGNAL_COLOR_LOW, SIGNAL_COLOR_HIGH);
                    var endColor = signal2color(cursor.tint - 1, SIGNAL_COLOR_LOW, SIGNAL_COLOR_HIGH);

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

                var startColor = signal2color(trailer.tint - 1, SIGNAL_COLOR_LOW, SIGNAL_COLOR_HIGH);
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
