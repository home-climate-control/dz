package net.sf.dz3.view.swing.thermostat;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.time.Clock;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import net.sf.dz3.controller.DataSet;
import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.util.Interval;

/**
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2020
 *
 * VT: NOTE: squid:S110 - I don't care, I didn't create those parents, Sun did. I need mine.
 */
@SuppressWarnings("squid:S110")
public class Chart2016 extends AbstractChart {

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
    private int width = 0;

    private final transient Map<String, Averager> channel2avg = new HashMap<>();

    public Chart2016(Clock clock, long chartLengthMillis) {

        super(clock, chartLengthMillis);
    }

    @Override
    public synchronized void consume(DataSample<TintedValueAndSetpoint> signal) {

        if (signal == null || signal.sample == null) {
            throw new IllegalArgumentException("signal or sample are null: " + signal);
        }

        String channel = signal.sourceName;

        if (record(channel, signal)) {

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
    private boolean record(String channel, DataSample<TintedValueAndSetpoint> signal) {

        adjustVerticalLimits(signal.timestamp, signal.sample.value, signal.sample.setpoint);

        synchronized (AbstractChart.class) {

            if (width != globalWidth) {

                // Chart size changed, need to adjust the buffer

                width = globalWidth;

                long step = chartLengthMillis / width;

                logger.info("new width {}, {}ms per pixel", width, step);

                // We lose one sample this way, might want to improve it later, for now, no big deal

                channel2avg.put(channel, new Averager(step));

                return true;
            }
        }

        if (width == 0) {

            // There's nothing we can do before the width is set.
            // It's not even worth it to record the value.

            // Please repaint.
            logger.info("please repaint");
            return true;
        }

        Averager avg = channel2avg.get(channel);
        TintedValue tv = avg.record(signal);

        if (tv == null) {

            // The average is still being calculated, nothing to do
            return false;
        }

        DataSet<TintedValue> dsValues = channel2dsValue.get(channel);
        DataSet<Double> dsSetpoints = channel2dsSetpoint.get(channel);

        if (dsValues == null) {

            dsValues = new DataSet<>(chartLengthMillis);
            channel2dsValue.put(channel, dsValues);

            // Most definitely, setpoints aren't there either

            dsSetpoints = new DataSet<>(chartLengthMillis);
            channel2dsSetpoint.put(channel, dsSetpoints);
        }

        dsValues.record(signal.timestamp, tv);
        dsSetpoints.record(signal.timestamp, signal.sample.setpoint);

        return true;
    }

    @Override
    protected void checkWidth(Dimension boundary) {

        // Chart size *can* change during runtime - see +/- Console#ResizeKeyListener.

        synchronized (AbstractChart.class) {

            if (globalWidth != boundary.width) {

                logger.info("width changed from {} to {}", globalWidth, boundary.width);

                globalWidth = boundary.width;

                long step = chartLengthMillis / globalWidth;

                logger.info("ms per pixel: {}", step);
            }
        }

    }

    @Override
    protected void paintChart(Graphics2D g2d, Dimension boundary, Insets insets,
            long now, double xScale, long xOffset, double yScale, double yOffset,
            String channel, DataSet<TintedValue> dsValues, DataSet<Double> dsSetpoints) {

        // Setpoint history is rendered over the value history

        paintValues(g2d, boundary, insets, now, xScale, xOffset, yScale, yOffset, channel, dsValues);
        paintSetpoints(g2d, boundary, insets, now, xScale, xOffset, yScale, yOffset, channel, dsSetpoints);
    }

    private void paintValues(Graphics2D g2d, Dimension boundary, Insets insets,
            long now, double xScale, long xOffset, double yScale, double yOffset,
            String channel, DataSet<TintedValue> ds) {

        Long timeTrailer = null;
        TintedValue trailer = null;

        for (Iterator<Entry<Long, TintedValue>> di = ds.entryIterator(); di.hasNext();) {

            Entry<Long, TintedValue> entry = di.next();
            long timeNow = entry.getKey();
            TintedValue cursor = entry.getValue();

            if (timeTrailer != null) {

                double x0 = (timeTrailer - xOffset) * xScale + insets.left;
                double y0 = (yOffset - trailer.value) * yScale + insets.top;

                double x1 = (timeNow - xOffset) * xScale + insets.left;
                double y1 = (yOffset - cursor.value) * yScale + insets.top;

                // Decide whether the line is alive or dead

                if (timeNow - timeTrailer > DEAD_TIMEOUT) {

                    // It's dead, all right
                    // Paint the horizontal line in dead color and skew the x0 so the next part will be painted vertical

                    Color startColor = signal2color(trailer.tint - 1, SIGNAL_COLOR_LOW, SIGNAL_COLOR_HIGH);

                    // End color differs from the start in alpha, not hue - this plays nicer with backgrounds
                    // Even though this is a memory allocation, it won't affect performance since [hopefully]
                    // there'll be just a few dead drops

                    Color endColor = new Color(startColor.getRed(), startColor.getGreen(), startColor.getBlue(), 64);

                    drawGradientLine(g2d, x0, y0, x1, y0, startColor, endColor, cursor.emphasize);

                    x0 = x1;
                }

                Color startColor = signal2color(trailer.tint - 1, SIGNAL_COLOR_LOW, SIGNAL_COLOR_HIGH);
                Color endColor = signal2color(cursor.tint - 1, SIGNAL_COLOR_LOW, SIGNAL_COLOR_HIGH);

                drawGradientLine(g2d, x0, y0, x1, y1, startColor, endColor, cursor.emphasize);
            }

            timeTrailer = timeNow;
            trailer = cursor;
        }

        if (timeTrailer != null && now - timeTrailer > DEAD_TIMEOUT) {

            // There's a gap on the right, let's fill it

            double x0 = (timeTrailer - xOffset) * xScale + insets.left;
            double x1 = (now - xOffset) * xScale + insets.left;
            double y = (yOffset - trailer.value) * yScale + insets.top;

            Color startColor = signal2color(trailer.tint - 1, SIGNAL_COLOR_LOW, SIGNAL_COLOR_HIGH);
            Color endColor = getBackground();

            drawGradientLine(g2d, x0, y, x1, y, startColor, endColor, false);
        }
    }

    private void paintSetpoints(Graphics2D g2d, Dimension boundary, Insets insets,
            long now, double xScale, long xOffset, double yScale, double yOffset,
            String channel, DataSet<Double> ds) {

        Color startColor = new Color(SETPOINT_COLOR.getRed(), SETPOINT_COLOR.getGreen(), SETPOINT_COLOR.getBlue(), 64);
        Color endColor = SETPOINT_COLOR;

        // VT: FIXME: At this point, we're making a second pass over ds (heavily redundant) and wasting a lot of time
        // to do very little (it may so happen that there's been just one setpoint value over the whole chart).
        // However, commit 369f1344a58fbceaa5a174f2def81200954ae9a3 (adding setpoint to TintedValue) was extremely
        // wasteful already, so let's just get the visuals right for now, and find a way of changing the data flow
        // in a way that would a) not break the DataSink<TintedValue> interface b) not introduce memory chatter
        // during fast processing cycles. It will most probably involve having a separate efficient structure
        // to hold setpoint history, so this method is exactly where rendering belongs.

        Long timeTrailer = null;
        Double trailer = null;

        for (Iterator<Entry<Long, Double>> di = ds.entryIterator(); di.hasNext();) {

            Entry<Long, Double> entry = di.next();
            long timeNow = entry.getKey();
            Double cursor = entry.getValue();

            if (timeTrailer == null) {

                timeTrailer = timeNow;
                trailer = cursor;

                continue;
            }

            if (Double.compare(trailer, cursor) != 0 || !di.hasNext()) {

                // Setpoint changed, or we're at the last sample, time to draw the line

                double x0 = (timeTrailer - xOffset) * xScale + insets.left;
                double x1 = (timeNow - xOffset) * xScale + insets.left;
                double y = (yOffset - trailer) * yScale + insets.top;

                drawGradientLine(g2d, x0, y, x1, y, startColor, endColor, false);

                timeTrailer = timeNow;
                trailer = cursor;
            }
        }
    }

    /**
     * Averaging tool.
     */
    private class Averager {

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
        @SuppressWarnings("squid:S2629")
        public TintedValue record(DataSample<? extends TintedValue> signal) {

            if (timestamp == null) {
                timestamp = signal.timestamp;
            }

            long age = signal.timestamp - timestamp;

            if ( age < expirationInterval) {

                count++;
                valueAccumulator += signal.sample.value;
                tintAccumulator += signal.sample.tint;
                emphasizeAccumulator += signal.sample.emphasize ? 1 : 0;

                return null;
            }

            // VT: NOTE: squid:S2629 - this happens once in 25-40 seconds, acceptable loss

            logger.debug("RingBuffer: flushing at {}", Interval.toTimeInterval(age));

            TintedValue result = new TintedValue(valueAccumulator / count, tintAccumulator / count, emphasizeAccumulator > 0);

            count = 1;
            valueAccumulator = signal.sample.value;
            tintAccumulator = signal.sample.tint;
            emphasizeAccumulator = 0;
            timestamp = signal.timestamp;

            return result;
        }
    }
}
