package net.sf.dz3.view.swing;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import com.homeclimatecontrol.jukebox.util.Interval;
import net.sf.dz3.view.swing.zone.AbstractZoneChart;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.time.Clock;

public abstract class AbstractChart<T> extends JPanel implements DataSink<T> {

    protected static final Stroke strokeSingle = new BasicStroke();
    protected static final Stroke strokeDouble = new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, null, 0.0f);

    protected final transient Logger logger = LogManager.getLogger();

    /**
     * Grid color.
     *
     * Default is dark gray.
     */
    protected static final Color gridColor = Color.darkGray;

    /**
     * Clock used.
     */
    protected final transient Clock clock;

    /**
     * Chart length, in milliseconds.
     */
    protected long chartLengthMillis;

    /**
     * Dead timeout, in milliseconds.
     *
     * It is possible that the data readings don't come for a long time, in this
     * case the chart becomes funny - there will be interruptions at the right,
     * but when the data becomes available quite a bit longer, there'll be a
     * change in appearance - what should have been a horizontal line with a
     * step, will become a slightly sloped line. In order to avoid this, the
     * gaps longer than the dead timeout will be painted differently.
     *
     * Default is one minute.
     */
    protected static final long DEAD_TIMEOUT = 1000L * 60;

    /**
     * Horizontal grid spacing.
     *
     * Vertical grid lines will be painted every <code>timeSpacing</code>
     * milliseconds. Default is 30 minutes.
     */
    protected static final long SPACING_TIME = 1000L * 60 * 30;

    /**
     * Vertical grid spacing.
     *
     * Horizontal grid lines will be painted every <code>valueSpacing</code>
     * units. Default is 1.0.
     */
    protected static final double SPACING_VALUE = 1.0;

    /**
     * How much space to leave between the chart and the edge.
     */
    protected static final double PADDING = 0.2;

    /**
     * Maximum known data value.
     */
    protected Double dataMax = null;

    /**
     * Minimum known data value.
     */
    protected Double dataMin = null;

    /**
     * Timestamp on {@link #dataMin} or {@link #dataMax}, whichever is younger.
     *
     * @see #adjustVerticalLimits(long, double, double)
     */
    private Long minmaxTime = null;

    /**
     * Amount of extra time to wait before {@link #recalculateVerticalLimits()
     * recalculating} the limits.
     *
     * Chances are, new min/max values will be pretty close to old, so unless
     * this value is used, recalculation will be happening more often than
     * necessary.
     */
    protected static final double MINMAX_OVERHEAD = 1.1;

    /**
     * Chart width in pixels for all the charts. Undefined until the first time
     * {@link #paintCharts(Graphics2D, Dimension, Insets, long, double, long, double, double)}
     * for any instance of this class is called.
     *
     * Making it static is ugly, but gets the job done - all the charts have the same width.
     */
    private static int globalWidth = 0;

    /**
     * Chart width of this instance.
     *
     * @see AbstractChart#getGlobalWidth()
     * @see #paintCharts(Graphics2D, Dimension, Insets, long, double, long, double, double)
     */
    protected int localWidth = 0;

    protected AbstractChart(Clock clock, long chartLengthMillis) {

        if (chartLengthMillis < 1000 * 10) {
            throw new IllegalArgumentException("Unreasonably short chart length " + chartLengthMillis + "ms");
        }

        this.clock = clock;
        this.chartLengthMillis = chartLengthMillis;
    }

    @Override
    public synchronized void paintComponent(Graphics g) {

        // VT: NOTE: Consider replacing this with a Marker - careful, though, this is a time sensitive path
        long startTime = clock.instant().toEpochMilli();

        // Draw background
        super.paintComponent(g);

        var g2d = (Graphics2D) g;
        var boundary = getSize();
        var insets = getInsets();

        paintBackground(g2d, boundary, insets);

        var now = clock.instant().toEpochMilli();
        var xScale = (double) (boundary.width - insets.left - insets.right) / (double) chartLengthMillis;
        var xOffset = now - chartLengthMillis;

        paintTimeGrid(g2d, boundary, insets, now, xScale, xOffset);

        checkWidth(boundary);

        if (!isDataAvailable()) {
            return;
        }

        var yScale = (boundary.height - insets.bottom - insets.top) / (dataMax - dataMin + PADDING * 2);
        var yOffset = dataMax + PADDING;

        paintValueGrid(g2d, boundary, insets, now, xScale, xOffset, yScale, yOffset);
        paintCharts(g2d, boundary, insets, now, xScale, xOffset, yScale, yOffset);

        logger.info("Painted in {}ms", (clock.instant().toEpochMilli() - startTime));
    }

    private void paintBackground(Graphics2D g2d, Dimension boundary, Insets insets) {

        g2d.setPaint(getBackground());

        var background = new Rectangle2D.Double(
                insets.left, insets.top,
                (double)boundary.width - insets.right - insets.left,
                (double)boundary.height - insets.bottom - insets.top);

        g2d.fill(background);
    }

    private void paintTimeGrid(Graphics2D g2d, Dimension boundary, Insets insets, long now, double xScale, long xOffset) {

        var originalStroke = (BasicStroke) g2d.getStroke();

        g2d.setPaint(gridColor);

        var gridDash = new float[] { 2, 2 };

        var gridStroke = new BasicStroke(
                originalStroke.getLineWidth(), originalStroke.getEndCap(),
                originalStroke.getLineJoin(),
                originalStroke.getMiterLimit(), gridDash,
                originalStroke.getDashPhase());

        g2d.setStroke(gridStroke);

        for (long timeOffset = now - SPACING_TIME; timeOffset > now - chartLengthMillis; timeOffset -= SPACING_TIME) {

            double gridX = (timeOffset - xOffset) * xScale + insets.left;

            drawGradientLine(g2d,
                    gridX, insets.top,
                    gridX, (double)boundary.height - insets.bottom - 1,
                    getBackground(), Color.GRAY.darker().darker(), false);
        }

        g2d.setStroke(originalStroke);
    }

    @SuppressWarnings("squid:S107")
    private void paintValueGrid(
            Graphics2D g2d, Dimension boundary, Insets insets, long now,
            double xScale, long xOffset, double yScale, double yOffset) {

        // VT: NOTE: squid:S107 - following this rule will hurt performance, so no.

        var originalStroke = (BasicStroke) g2d.getStroke();

        g2d.setPaint(gridColor);

        var gridDash = new float[] { 2, 2 };

        var gridStroke = new BasicStroke(
                originalStroke.getLineWidth(), originalStroke.getEndCap(),
                originalStroke.getLineJoin(),
                originalStroke.getMiterLimit(), gridDash,
                originalStroke.getDashPhase());

        // The zero line gets painted with the default stroke

        g2d.setStroke(originalStroke);

        var gridY = yOffset * yScale + insets.top;

        Line2D gridLine = new Line2D.Double(
                insets.left,
                gridY,
                (double)boundary.width - insets.right - 1,
                gridY);

        g2d.draw(gridLine);

        // All the rest of the grid lines get painted with a dashed line

        g2d.setStroke(gridStroke);

        var halfWidth = (boundary.width - insets.right - 1) / 2d;

        for (var valueOffset = SPACING_VALUE; valueOffset < dataMax + PADDING; valueOffset += SPACING_VALUE) {

            gridY = (yOffset - valueOffset) * yScale + insets.top;

            drawGradientLine(g2d,
                    insets.left, gridY,
                    halfWidth, gridY,
                    Color.GRAY.darker().darker(), getBackground(),
                    false);

            drawGradientLine(g2d,
                    halfWidth, gridY,
                    (double)boundary.width - insets.right - 1, gridY,
                    getBackground(), Color.GRAY.darker().darker(),
                    false);
        }

        for (var valueOffset = -SPACING_VALUE; valueOffset > dataMin - PADDING; valueOffset -= SPACING_VALUE) {

            gridY = (yOffset - valueOffset) * yScale + insets.top;

            drawGradientLine(g2d,
                    insets.left, gridY,
                    halfWidth, gridY,
                    getBackground(), Color.GRAY.darker().darker(),
                    false);

            drawGradientLine(g2d,
                    halfWidth, gridY,
                    (double)boundary.width - insets.right - 1, gridY,
                    getBackground(), Color.GRAY.darker().darker(),
                    false);
        }

        g2d.setStroke(originalStroke);
    }

    /**
     * Draw the gradient line between given points and given colors.
     *
     * @param emphasize {@code true} if this particular line has to stand out.
     * Exact way of emphasizing is left to the implementation.
     */
    @SuppressWarnings("squid:S107")
    protected final void drawGradientLine(
            Graphics2D g2d,
            double x0, double y0, double x1, double y1,
            Color startColor, Color endColor,
            boolean emphasize) {

        // VT: NOTE: squid:S107 - following this rule will hurt performance, so no.

        var gp = new GradientPaint(
                (int) x0, (int) y0, startColor,
                (int) x1, (int) y1, endColor);
        Line2D line = new Line2D.Double(x0, y0, x1, y1);

        g2d.setPaint(gp);
        g2d.setStroke(emphasize ? strokeDouble : strokeSingle);
        g2d.draw(line);
    }

    /**
     * Adjust the vertical limits, if necessary.
     *
     * @param timestamp Value timestamp.
     * @param value Incoming data element.
     * @param setpoint Incoming setpoint.
     *
     * @see #dataMax
     * @see #dataMin
     */
    protected final void adjustVerticalLimits(long timestamp, double value, double setpoint) {
        adjustVerticalLimits(timestamp, value);
        adjustVerticalLimitsExt(timestamp, setpoint);
    }

    protected final void adjustVerticalLimits(long timestamp, double value) {

        if ((minmaxTime != null) && (timestamp - minmaxTime > chartLengthMillis * MINMAX_OVERHEAD)) {

            logger.info("minmax too old ({}), recalculating", () -> Interval.toTimeInterval(timestamp - minmaxTime));

            // Total recalculation is required
            var limits = recalculateVerticalLimits();

            dataMin = limits.min;
            dataMax = limits.max;
            minmaxTime = limits.minmaxTime;
        }

        // Treating minmaxTime like this still allows for lopsided chart if a long up or down trend continues,
        // but we probably do want to know about that, so let's just make a note and ignore it for the moment

        if (dataMax == null || value > dataMax) {
            dataMax = value;
            minmaxTime = timestamp;
        }

        if (dataMin == null || value < dataMin) {
            dataMin = value;
            minmaxTime = timestamp;
        }
    }

    protected final void adjustVerticalLimitsExt(long timestamp, double value) {

        // By this time, dataMin and dataMax are no longer nulls - see the call stack

        if (value > dataMax) {
            dataMax = value;
            minmaxTime = timestamp;
        }

        if (value < dataMin) {
            dataMin = value;
            minmaxTime = timestamp;
        }
    }

    protected void checkWidth(Dimension boundary) {

        // Chart size *can* change during runtime - see +/- Console#ResizeKeyListener.

        synchronized (AbstractZoneChart.class) {

            if (globalWidth != boundary.width) {

                logger.info("width changed from {} to {}", globalWidth, boundary.width);

                globalWidth = boundary.width;

                var step = chartLengthMillis / globalWidth;

                logger.info("ms per pixel: {}", step);
            }
        }
    }

    protected int getGlobalWidth() {
        return globalWidth;
    }

    protected abstract boolean isDataAvailable();
    protected abstract Limits recalculateVerticalLimits();

    @SuppressWarnings("squid:S107")
    protected abstract void paintCharts(
            Graphics2D g2d, Dimension boundary, Insets insets, long now,
            double xScale, long xOffset, double yScale, double yOffset);

    protected static class Limits {
        public final Double min;
        public final Double max;
        public final Long minmaxTime;

        public Limits(Double min, Double max, Long minmaxTime) {
            this.min = min;
            this.max = max;
            this.minmaxTime = minmaxTime;
        }

    }
}
