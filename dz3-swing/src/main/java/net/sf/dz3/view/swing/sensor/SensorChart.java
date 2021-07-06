package net.sf.dz3.view.swing.sensor;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.util.Interval;
import net.sf.dz3.controller.DataSet;
import net.sf.dz3.view.swing.AbstractChart;
import net.sf.dz3.view.swing.ColorScheme;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.time.Clock;

public class SensorChart extends AbstractChart<Double> {

    private final transient DataSet<Double> values = new DataSet<>(chartLengthMillis);
    private Averager averager;

    protected SensorChart(Clock clock, long chartLengthMillis) {
        super(clock, chartLengthMillis);
    }

    @Override
    public void consume(DataSample<Double> signal) {

        if (signal == null || signal.sample == null) {
            throw new IllegalArgumentException("signal or sample are null: " + signal);
        }

        if (append(signal)) {
            repaint();
        }
    }

    private boolean append(DataSample<Double> signal) {

        adjustVerticalLimits(signal.timestamp, signal.sample);

        synchronized (AbstractChart.class) {

            if (localWidth != getGlobalWidth()) {

                // Chart size changed, need to adjust the buffer
                localWidth = getGlobalWidth();

                var step = chartLengthMillis / localWidth;

                logger.info("new width {}, {}ms per pixel", localWidth, step);

                // We lose one sample this way, might want to improve it later, for now, no big deal
                averager = new Averager(step);

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

        values.append(signal.timestamp, value);

        return true;
    }

    @Override
    protected boolean isDataAvailable() {
        return dataMax != null && dataMin != null;
    }

    @Override
    protected Limits recalculateVerticalLimits() {
        return null;
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

                if (timeNow - timeTrailer > DEAD_TIMEOUT) {

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

        if (timeTrailer != null && now - timeTrailer > DEAD_TIMEOUT) {

            // There's a gap on the right, let's fill it

            var x0 = (timeTrailer - xOffset) * xScale + insets.left;
            var x1 = (now - xOffset) * xScale + insets.left;
            var y = (yOffset - trailer) * yScale + insets.top;

            var startColor = ColorScheme.offMap.sensorStale;
            var endColor = getBackground();

            drawGradientLine(g2d, x0, y, x1, y, startColor, endColor, false);
        }
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
        public Double append(DataSample<? extends Double> signal) {

            if (timestamp == null) {
                timestamp = signal.timestamp;
            }

            var age = signal.timestamp - timestamp;

            if ( age < expirationInterval) {

                count++;
                valueAccumulator += signal.sample;

                return null;
            }

            logger.debug("RingBuffer: flushing at {}", () -> Interval.toTimeInterval(age));

            var result = valueAccumulator / count;

            count = 1;
            valueAccumulator = signal.sample;
            timestamp = signal.timestamp;

            return result;
        }
    }
}
