package net.sf.dz3.view.swing.sensor;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.util.Interval;
import net.sf.dz3.controller.DataSet;
import net.sf.dz3.view.swing.AbstractChart;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;
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
        return false;
    }

    @Override
    protected Limits recalculateVerticalLimits() {
        return null;
    }

    @Override
    protected void paintCharts(Graphics2D g2d, Dimension boundary, Insets insets, long now, double xScale, long xOffset, double yScale, double yOffset) {

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
