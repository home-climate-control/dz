package net.sf.dz3r.view.swing.sensor;


import net.sf.dz3r.common.DataSet;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.view.swing.AbstractChart;
import net.sf.dz3r.view.swing.ColorScheme;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.time.Clock;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;

public class SensorChart extends AbstractChart<Double, Void> {

    private final transient DataSet<Double> values = new DataSet<>(chartLengthMillis);
    private transient DoubleAverager averager;

    protected SensorChart(Clock clock, long chartLengthMillis) {
        super(clock, chartLengthMillis, false);
    }

    @Override
    protected void update() {

        if (append(getSignal())) {
            repaint();
        }
    }
    private boolean append(Signal<Double, Void> signal) {

        adjustVerticalLimits(signal.timestamp.toEpochMilli(), signal.getValue());

        synchronized (AbstractChart.class) {

            if (localWidth != getGlobalWidth()) {

                // Chart size changed, need to adjust the buffer
                localWidth = getGlobalWidth();

                var step = chartLengthMillis / localWidth;

                logger.info("new width {}, {}ms per pixel", localWidth, step);

                // We lose one sample this way, might want to improve it later, for now, no big deal
                averager = new DoubleAverager(step);

                return true;
            }
        }

        if (localWidth == 0) {

            // There's nothing we can do before the width is set.
            // It's not even worth it to record the value.

            logger.info("please repaint");
            return true;
        }

        var value = averager.append(signal);

        if (value == null) {
            // The average is still being calculated, nothing to do
            return false;
        }

        values.append(signal.timestamp.toEpochMilli(), value);

        return true;
    }

    @Override
    protected boolean isDataAvailable() {
        return dataMax != null && dataMin != null;
    }

    @Override
    protected Limits recalculateVerticalLimits() {
        var startTime = clock.instant().toEpochMilli();

        Double min = null;
        Double max = null;
        Long minmaxTime = null;

        for (Iterator<Map.Entry<Long, Double>> i2 = values.entryIterator(); i2.hasNext(); ) {

            var entry = i2.next();
            var timestamp = entry.getKey();
            var value = entry.getValue();

            if (max == null || value > max) {
                max = value;
                minmaxTime = timestamp;
            }

            if (min == null || value < min) {
                min = value;
                minmaxTime = timestamp;
            }
        }

        var result = new Limits(min, max, minmaxTime);

        logger.info("Recalculated in {}ms", (clock.instant().toEpochMilli() - startTime));
        logger.info("New minmaxTime set to + {}", () -> Duration.ofMillis(clock.instant().toEpochMilli() - result.minmaxTime));

        return result;
    }

    @Override
    protected void paintCharts(Graphics2D g2d, Dimension boundary, Insets insets,
                               long now, double xScale, long xOffset, double yScale, double yOffset) {

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        paintValues(g2d, insets, now, xScale, xOffset, yScale, yOffset, values);
    }

    private void paintValues(Graphics2D g2d, Insets insets,
                             long now, double xScale, long xOffset, double yScale, double yOffset,
                             DataSet<Double> ds) {
        Long timeTrailer = null;
        Double trailer = null;

        for (var di = ds.entryIterator(); di.hasNext();) {

            var entry = di.next();
            var timeNow = entry.getKey();
            var cursor = entry.getValue();

            if (timeTrailer != null) {

                var x0 = (timeTrailer - xOffset) * xScale + insets.left;
                var y0 = (yOffset - trailer) * yScale + insets.top;

                var x1 = (timeNow - xOffset) * xScale + insets.left;
                var y1 = (yOffset - cursor) * yScale + insets.top;

                // Decide whether the line is alive or dead

                if (timeNow - timeTrailer > DEAD_TIMEOUT.toMillis()) {

                    // It's dead, all right
                    // Paint the horizontal line in dead color and skew the x0 so the next part will be painted vertical
                    var startColor = ColorScheme.offMap.sensorStale;

                    // End color differs from the start in alpha, not hue - this plays nicer with backgrounds
                    // Even though this is a memory allocation, it won't affect performance since [hopefully]
                    // there'll be just a few dead drops
                    var endColor = new Color(startColor.getRed(), startColor.getGreen(), startColor.getBlue(), 64);

                    drawGradientLine(g2d, x0, y0, x1, y0, startColor, endColor, false);

                    x0 = x1;
                }

                g2d.setPaint(ColorScheme.offMap.sensorNormal);
                Line2D line = new Line2D.Double(x0, y0, x1, y1);
                g2d.draw(line);
            }

            timeTrailer = timeNow;
            trailer = cursor;
        }

        if (timeTrailer != null && now - timeTrailer > DEAD_TIMEOUT.toMillis()) {

            // There's a gap on the right, let's fill it

            var x0 = (timeTrailer - xOffset) * xScale + insets.left;
            var x1 = (now - xOffset) * xScale + insets.left;
            var y = (yOffset - trailer) * yScale + insets.top;

            var startColor = ColorScheme.offMap.sensorStale;
            var endColor = getBackground();

            drawGradientLine(g2d, x0, y, x1, y, startColor, endColor, false);
        }
    }

    private class DoubleAverager extends Averager<Double, Double> {

        private double valueAccumulator = 0;

        protected DoubleAverager(long expirationInterval) {
            super(expirationInterval);
        }

        @Override
        protected void accumulate(Double value) {
            valueAccumulator += value;
        }

        @Override
        protected Double complete(Double value, int count) {

            var result = valueAccumulator / count;

            valueAccumulator = value;

            return result;
        }
    }
}
