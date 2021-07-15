package net.sf.dz3.view.swing.unit;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.util.Interval;
import net.sf.dz3.controller.DataSet;
import net.sf.dz3.device.model.UnitRuntimePredictionSignal;
import net.sf.dz3.view.swing.AbstractChart;
import net.sf.dz3.view.swing.ColorScheme;
import net.sf.dz3.view.swing.DoubleAverager;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.Map;

public class UnitChart extends AbstractChart<UnitRuntimePredictionSignal> {

    /**
     * Defines how wide the unit chart is the moment it turns on.
     *
     * Runtimes less than 10 minutes are a sure sign of the unit being oversized,
     * and long runtimes are quite variable.
     *
     * Also, defines how much time is added to the chart when it is about to be filled up.
     */
    private static final Duration timeSpanIncrement = Duration.of(15, ChronoUnit.MINUTES);

    private final transient DataSet<UnitRuntimePredictionSignal> values = new DataSet<>(timeSpanIncrement.toMillis());
    private transient DoubleAverager averager;

    protected UnitChart(Clock clock) {
        super(clock, timeSpanIncrement.toMillis());
    }

    @Override
    public void consume(DataSample<UnitRuntimePredictionSignal> signal) {

        if (signal == null || signal.sample == null) {
            throw new IllegalArgumentException("signal or sample are null: " + signal);
        }

        if (append(signal)) {
            repaint();
        }
    }

    private boolean append(DataSample<UnitRuntimePredictionSignal> signal) {

        adjustVerticalLimits(signal.timestamp, signal.sample.demand);

        synchronized (AbstractChart.class) {

            // Side effect: localWidth becomes 0 if runtime exceeds a certain share of the screen width
            checkRuntime(signal);

            if (localWidth != getGlobalWidth()) {

                // Chart size changed, need to adjust the buffer
                localWidth = getGlobalWidth();

                var step = chartLengthMillis / localWidth;

                logger.info("new width {}, {}ms per pixel", localWidth, step);

                if (averager == null) {
                    averager = new DoubleAverager(step);
                } else {
                    averager.setExpirationInterval(step);
                }

                return true;
            }
        }

        if (localWidth == 0) {

            // There's nothing we can do before the width is set.
            // It's not even worth it to record the value.

            logger.info("please repaint");
            return true;
        }

        if (signal.sample.uptime == 0) {

            // The unit is off, return to initial state

            values.clear();
            chartLengthMillis = timeSpanIncrement.toMillis();

            // Small inefficiency: only need to repaint if we just cleared the samples, but oh well
            return true;
        }

        var value = averager.append(signal.timestamp, signal.sample.demand);

        if (value == null) {
            // The average is still being calculated, nothing to do
            return false;
        }

        values.append(signal.timestamp, signal.sample);

        return true;
    }

    private void checkRuntime(DataSample<UnitRuntimePredictionSignal> signal) {

        if (signal.sample.uptime > chartLengthMillis * 0.8) {
            // Time to extend the chart
            chartLengthMillis = Duration.of(chartLengthMillis, ChronoUnit.MILLIS).plus(timeSpanIncrement).toMillis();
        }

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

        for (Iterator<Map.Entry<Long, UnitRuntimePredictionSignal>> i2 = values.entryIterator(); i2.hasNext(); ) {

            var entry = i2.next();
            var timestamp = entry.getKey();
            var value = entry.getValue();

            if (max == null || value.demand > max) {
                max = value.demand;
                minmaxTime = timestamp;
            }

            if (min == null || value.demand < min) {
                min = value.demand;
                minmaxTime = timestamp;
            }
        }

        var result = new Limits(min, max, minmaxTime);

        logger.info("Recalculated in {}ms", (clock.instant().toEpochMilli() - startTime));
        logger.info("New minmaxTime set to + {}", () -> Interval.toTimeInterval(clock.instant().toEpochMilli() - result.minmaxTime));

        return result;
    }

    @Override
    protected void paintCharts(Graphics2D g2d, Dimension boundary, Insets insets, long now, double xScale, long xOffset, double yScale, double yOffset) {

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        paintValues(g2d, insets, now, xScale, xOffset, yScale, yOffset, values);
    }
    private void paintValues(Graphics2D g2d, Insets insets,
                             long now, double xScale, long xOffset, double yScale, double yOffset,
                             DataSet<UnitRuntimePredictionSignal> ds) {
        Long timeTrailer = null;
        UnitRuntimePredictionSignal trailer = null;

        for (var di = ds.entryIterator(); di.hasNext();) {

            var entry = di.next();
            var timeNow = entry.getKey();
            var cursor = entry.getValue();

            if (timeTrailer != null) {

                var x0 = (timeTrailer - xOffset) * xScale + insets.left;
                var y0 = (yOffset - trailer.demand) * yScale + insets.top;

                var x1 = (timeNow - xOffset) * xScale + insets.left;
                var y1 = (yOffset - cursor.demand) * yScale + insets.top;

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
            var y = (yOffset - trailer.demand) * yScale + insets.top;

            var startColor = ColorScheme.offMap.sensorStale;
            var endColor = getBackground();

            drawGradientLine(g2d, x0, y, x1, y, startColor, endColor, false);
        }
    }
}
