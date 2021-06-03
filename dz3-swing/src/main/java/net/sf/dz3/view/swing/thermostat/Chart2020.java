package net.sf.dz3.view.swing.thermostat;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import net.sf.dz3.controller.DataSet;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.time.Clock;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 *
 * VT: NOTE: squid:S110 - I don't care, I didn't create those parents, Sun did. I need mine.
 */
@SuppressWarnings("squid:S110")
public class Chart2020 extends AbstractChart {

    private static final long serialVersionUID = 8739949924865459025L;

    /**
     * Chart width in pixels for all the charts. Undefined until the first time
     * {@link #paintCharts(Graphics2D, Dimension, Insets, long, double, long, double, double)}
     * for any instance of this class is called.
     *
     * Making it static is ugly, but gets the job done - the screen size will not change.
     */
    private static int globalWidth = 0;

    /**
     * Chart width of this instance.
     *
     * @see #globalWidth
     * @see #paintCharts(Graphics2D, Dimension, Insets, long, double, long, double, double)
     */
    private int localWidth = 0;

    private final transient Map<String, Averager> channel2avg = new HashMap<>();

    public Chart2020(Clock clock, long chartLengthMillis) {

        super(clock, chartLengthMillis);
    }

    @Override
    public synchronized void consume(DataSample<TintedValueAndSetpoint> signal) {

        if (signal == null || signal.sample == null) {
            throw new IllegalArgumentException("signal or sample are null: " + signal);
        }

        String channel = signal.sourceName;

        if (append(channel, signal)) {
            repaint();
        }
    }

    /**
     * Record the signal, properly spacing it out.
     *
     * @param channel Channel to use.
     * @param signal Signal to record.
     *
     * @return {@code true} if the component needs to be repainted.
     */
    private boolean append(String channel, DataSample<TintedValueAndSetpoint> signal) {

        adjustVerticalLimits(signal.timestamp, signal.sample.value, signal.sample.setpoint);

        synchronized (AbstractChart.class) {

            if (localWidth != globalWidth) {

                // Chart size changed, need to adjust the buffer
                localWidth = globalWidth;

                var step = chartLengthMillis / localWidth;

                logger.info("new width {}, {}ms per pixel", localWidth, step);

                // We lose one sample this way, might want to improve it later, for now, no big deal
                channel2avg.put(channel, new Averager(step));

                return true;
            }
        }

        if (localWidth == 0) {

            // There's nothing we can do before the width is set.
            // It's not even worth it to record the value.

            logger.info("please repaint");
            return true;
        }

        var averager = channel2avg.get(channel);
        var tintedValue = averager.append(signal);

        if (tintedValue == null) {
            // The average is still being calculated, nothing to do
            return false;
        }

        var dsValues = channel2dsValue.computeIfAbsent(channel, v -> new DataSet<>(chartLengthMillis));
        var dsSetpoints = channel2dsSetpoint.computeIfAbsent(channel, v -> new DataSet<>(chartLengthMillis));

        dsValues.append(signal.timestamp, tintedValue, true);
        dsSetpoints.append(signal.timestamp, signal.sample.setpoint, true);

        return true;
    }

    @Override
    protected void checkWidth(Dimension boundary) {

        // Chart size *can* change during runtime - see +/- Console#ResizeKeyListener.

        synchronized (AbstractChart.class) {

            if (globalWidth != boundary.width) {

                logger.info("width changed from {} to {}", globalWidth, boundary.width);

                globalWidth = boundary.width;

                var step = chartLengthMillis / globalWidth;

                logger.info("ms per pixel: {}", step);
            }
        }
    }

    @Override
    protected void paintChart(Graphics2D g2d, Dimension boundary, Insets insets,
            long now, double xScale, long xOffset, double yScale, double yOffset,
            String channel, DataSet<TintedValue> dsValues, DataSet<Double> dsSetpoints) {

        // Setpoint history is rendered over the value history
        paintValues(g2d, insets, now, xScale, xOffset, yScale, yOffset, dsValues);
        paintSetpoints(g2d, insets, xScale, xOffset, yScale, yOffset, dsSetpoints);
    }

    @SuppressWarnings("squid:S107")
    private void paintValues(Graphics2D g2d, Insets insets,
                             long now, double xScale, long xOffset, double yScale, double yOffset,
                             DataSet<TintedValue> ds) {

        Long timeTrailer = null;
        TintedValue trailer = null;

        for (Iterator<Entry<Long, TintedValue>> di = ds.entryIterator(); di.hasNext();) {

            var entry = di.next();
            var timeNow = entry.getKey();
            var cursor = entry.getValue();

            if (timeTrailer != null) {

                var x0 = (timeTrailer - xOffset) * xScale + insets.left;
                var y0 = (yOffset - trailer.value) * yScale + insets.top;

                var x1 = (timeNow - xOffset) * xScale + insets.left;
                var y1 = (yOffset - cursor.value) * yScale + insets.top;

                // Decide whether the line is alive or dead

                if (timeNow - timeTrailer > DEAD_TIMEOUT) {

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

        if (timeTrailer != null && now - timeTrailer > DEAD_TIMEOUT) {

            // There's a gap on the right, let's fill it

            var x0 = (timeTrailer - xOffset) * xScale + insets.left;
            var x1 = (now - xOffset) * xScale + insets.left;
            var y = (yOffset - trailer.value) * yScale + insets.top;

            var startColor = signal2color(trailer.tint - 1, SIGNAL_COLOR_LOW, SIGNAL_COLOR_HIGH);
            var endColor = getBackground();

            drawGradientLine(g2d, x0, y, x1, y, startColor, endColor, false);
        }
    }

    private void paintSetpoints(Graphics2D g2d, Insets insets,
                                double xScale, long xOffset, double yScale, double yOffset,
                                DataSet<Double> ds) {

        var startColor = new Color(SETPOINT_COLOR.getRed(), SETPOINT_COLOR.getGreen(), SETPOINT_COLOR.getBlue(), 64);
        var endColor = SETPOINT_COLOR; // NOSONAR Retained for clarity

        Long timeTrailer = null;

        for (Iterator<Entry<Long, Double>> di = ds.entryIterator(); di.hasNext();) {

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

            drawGradientLine(g2d, x0, y, x1, y, startColor, endColor, false);

            timeTrailer = timeNow;
        }
    }
}
