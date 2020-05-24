package net.sf.dz3.view.swing.thermostat;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.time.Clock;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.sf.dz3.controller.DataSet;
import net.sf.jukebox.util.Interval;

/**
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2020
 */
public abstract class AbstractChart2009 extends AbstractChart {

    private static final long serialVersionUID = -8584582539155161184L;

    private static final Stroke strokeSingle = new BasicStroke();
    private static final Stroke strokeDouble = new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, null, 0.0f);

    protected final transient Logger logger = LogManager.getLogger(getClass());

    protected final transient SortedMap<String, DataSet<TintedValue>> channel2dsValue = new TreeMap<>();
    protected final transient SortedMap<String, DataSet<Double>> channel2dsSetpoint = new TreeMap<>();

    /**
     * Grid color.
     *
     * Default is dark gray.
     */
    protected static final Color gridColor = Color.darkGray;

    /**
     * Clock used.
     */
    private final Clock clock;

    /**
     * Chart length, in milliseconds.
     */
    protected final long chartLengthMillis;

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
     * @see #adjustVerticalLimits(double)
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

    protected static final Color SIGNAL_COLOR_LOW = Color.GREEN;
    protected static final Color SIGNAL_COLOR_HIGH = Color.RED;
    protected static final Color SETPOINT_COLOR = Color.YELLOW;

    public AbstractChart2009(Clock clock, long chartLengthMillis) {

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

        Graphics2D g2d = (Graphics2D) g;
        Dimension boundary = getSize();
        Insets insets = getInsets();

        paintBackground(g2d, boundary, insets);

        long now = clock.instant().toEpochMilli();
        double xScale = (double) (boundary.width - insets.left - insets.right) / (double) chartLengthMillis;
        long xOffset = now - chartLengthMillis;

        paintTimeGrid(g2d, boundary, insets, now, xScale, xOffset);

        // VT: FIXME: Ugly hack.
        checkWidth(boundary);

        if (!isDataAvailable()) {
            return;
        }

        double yScale = (boundary.height - insets.bottom - insets.top) / (dataMax - dataMin + PADDING * 2);
        double yOffset = dataMax + PADDING;

        paintValueGrid(g2d, boundary, insets, now, xScale, xOffset, yScale, yOffset);

        paintCharts(g2d, boundary, insets, now, xScale, xOffset, yScale, yOffset);

        logger.info("Painted in {}ms", (clock.instant().toEpochMilli() - startTime));
    }

    protected abstract void checkWidth(Dimension boundary);

    @SuppressWarnings("squid:S1126")
    protected final boolean isDataAvailable() {

        // VT: NOTE: squid:S1126 - following this rule will hurt readability, so no.

        if (channel2dsValue.isEmpty() || dataMax == null || dataMin == null) {

            // No data consumed yet
            return false;
        }

        return true;

    }

    @SuppressWarnings("squid:S107")
    protected final void paintCharts(
            Graphics2D g2d, Dimension boundary, Insets insets, long now,
            double xScale, long xOffset, double yScale, double yOffset) {

        // VT: NOTE: squid:S107 - following this rule will hurt performance, so no.

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (Iterator<Entry<String, DataSet<TintedValue>>> iv = channel2dsValue.entrySet().iterator(); iv.hasNext(); ) {

            // VT: FIXME: Implement depth ordering

            Entry<String, DataSet<TintedValue>> entry = iv.next();
            String channel = entry.getKey();
            DataSet<TintedValue> dsValues = entry.getValue();
            DataSet<Double> dsSetpoints = channel2dsSetpoint.get(channel);

            paintChart(g2d, boundary, insets, now, xScale, xOffset, yScale, yOffset, channel, dsValues, dsSetpoints);
        }
    }

    /**
     * VT: NOTE: squid:S107 - following this rule will hurt performance, so no.
     */
    @SuppressWarnings("squid:S107")
    protected abstract void paintChart(
            Graphics2D g2d, Dimension boundary, Insets insets, long now,
            double xScale, long xOffset, double yScale, double yOffset,
            String channel, DataSet<TintedValue> dsValues, DataSet<Double> dsSetpoints);

    private void paintBackground(Graphics2D g2d, Dimension boundary, Insets insets) {

        g2d.setPaint(getBackground());

        Rectangle2D.Double background = new Rectangle2D.Double(
                insets.left, insets.top,
                (double)boundary.width - insets.right - insets.left,
                (double)boundary.height - insets.bottom - insets.top);

        g2d.fill(background);
    }

    private void paintTimeGrid(Graphics2D g2d, Dimension boundary, Insets insets, long now, double xScale, long xOffset) {

        BasicStroke originalStroke = (BasicStroke) g2d.getStroke();

        g2d.setPaint(gridColor);

        float[] gridDash = { 2, 2 };

        BasicStroke gridStroke = new BasicStroke(
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

        BasicStroke originalStroke = (BasicStroke) g2d.getStroke();

        g2d.setPaint(gridColor);

        float[] gridDash = { 2, 2 };

        BasicStroke gridStroke = new BasicStroke(
                originalStroke.getLineWidth(), originalStroke.getEndCap(),
                originalStroke.getLineJoin(),
                originalStroke.getMiterLimit(), gridDash,
                originalStroke.getDashPhase());

        // The zero line gets painted with the default stroke

        g2d.setStroke(originalStroke);

        double gridY = yOffset * yScale + insets.top;

        Line2D gridLine = new Line2D.Double(
                insets.left,
                gridY,
                (double)boundary.width - insets.right - 1,
                gridY);

        g2d.draw(gridLine);

        // All the rest of the grid lines get painted with a dashed line

        g2d.setStroke(gridStroke);

        double valueOffset = 0;
        double halfWidth = (boundary.width - insets.right - 1) / 2d;

        for (valueOffset = SPACING_VALUE; valueOffset < dataMax + PADDING; valueOffset += SPACING_VALUE) {

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

        for (valueOffset = -SPACING_VALUE; valueOffset > dataMin - PADDING; valueOffset -= SPACING_VALUE) {

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

        GradientPaint gp = new GradientPaint(
                (int) x0, (int) y0, startColor,
                (int) x1, (int) y1, endColor);
        Line2D line = new Line2D.Double(x0, y0, x1, y1);

        g2d.setPaint(gp);
        g2d.setStroke(emphasize ? strokeDouble : strokeSingle);
        g2d.draw(line);
    }

    private static Color[] signalCache = new Color[256];

    /**
     * Convert signal from -1 to +1 to color from low color to high color.
     *
     * @param signal Signal to convert to color.
     * @param low Color corresponding to -1 signal value.
     * @param high Color corresponding to +1 signal value.
     * @return
     */
    protected final Color signal2color(double signal, Color low, Color high) {

        signal = signal > 1 ? 1: signal;
        signal = signal < -1 ? -1 : signal;
        signal = (signal + 1) / 2;

        int index = (int) (signal * 255);

        synchronized (signalCache) {

            Color result = signalCache[index];

            if ( result == null) {

                float[] hsbLow = resolve(low);
                float[] hsbHigh = resolve(high);

                float h = transform(signal, hsbLow[0], hsbHigh[0]);
                float s = transform(signal, hsbLow[1], hsbHigh[1]);
                float b = transform(signal, hsbLow[2], hsbHigh[2]);

                result = new Color(Color.HSBtoRGB(h, s, b));
                signalCache[index] = result;
            }

            return result;
        }
    }

    private static class RGB2HSB {

        public final int rgb;
        public final float[] hsb;

        public RGB2HSB(int rgb, float[] hsb) {

            this.rgb = rgb;
            this.hsb = hsb;
        }
    }

    /**
     * Cache medium for {@link #resolve()}.
     *
     * According to "worse is better" rule, there's no error checking against
     * the array size - too expensive. In all likelihood, this won't grow beyond 2 entries.
     */
    private static RGB2HSB[] rgb2hsb = new RGB2HSB[16];

    /**
     * Resolve a possibly cached {@link Color#RGBtoHSB(int, int, int, float[])} result,
     * or compute it and store it for later retrieval if it hasn't been done.
     *
     * @param color Color to transform.
     * @return Transformation result.
     */
    private float[] resolve(Color color) {

        int rgb = color.getRGB();
        int offset = 0;

        for (; offset < rgb2hsb.length && rgb2hsb[offset] != null; offset++) {

            if (rgb == rgb2hsb[offset].rgb) {

                return rgb2hsb[offset].hsb;
            }
        }

        synchronized (rgb2hsb) {

            // VT: NOTE: Not the cleanest solution. It is possible that someone has just tried to do the same thing
            // and we'll end up writing the same value twice, but oh well, it's the same value in an array of size 16

            rgb2hsb[offset] = new RGB2HSB(rgb, Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null));
        }

        logger.info("RGB2HSB offset={}", offset );

        return rgb2hsb[offset].hsb;
    }

    /**
     * Get the point between the start and end values corresponding to the value of the signal.
     *
     * @param signal Signal value, from -1 to +1.
     * @param start Start point.
     * @param end End point.
     *
     * @return Desired position between the start and end points.
     */
    private float transform(double signal, float start, float end) {

        assert(signal <= 1);
        assert(signal >= -1);

        return (float) (start + signal * (end - start));
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
    @SuppressWarnings("squid:S2629")
    protected final void adjustVerticalLimits(long timestamp, double value, double setpoint) {

        if ((minmaxTime != null) && (timestamp - minmaxTime > chartLengthMillis * MINMAX_OVERHEAD)) {

            // VT: NOTE: squid:S2629 - give me a break, this will happen once in more than three hours

            logger.info("minmax too old ({}), recalculating", Interval.toTimeInterval(timestamp - minmaxTime));

            // Total recalculation is required

            recalculateVerticalLimits();
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

        // By this time, dataMin and dataMax are no longer nulls

        if (setpoint > dataMax) {

            dataMax = setpoint;
            minmaxTime = timestamp;
        }

        if (setpoint < dataMin) {

            dataMin = setpoint;
            minmaxTime = timestamp;
        }
    }

    /**
     * Calculate {@link #dataMin} and {@link #dataMax} based on all values available in {@link #channel2dsValue}.
     */
    @SuppressWarnings("squid:S2629")
    private synchronized void recalculateVerticalLimits() {

        long startTime = clock.instant().toEpochMilli();

        dataMin = null;
        dataMax = null;

        for (Iterator<DataSet<TintedValue>> i = channel2dsValue.values().iterator(); i.hasNext(); ) {

            DataSet<TintedValue> ds = i.next();

            for (Iterator<Entry<Long, TintedValue>> i2 = ds.entryIterator(); i2.hasNext(); ) {

                Entry<Long, TintedValue> entry = i2.next();
                Long timestamp = entry.getKey();
                TintedValue tv = entry.getValue();

                if (dataMax == null || tv.value > dataMax) {

                    dataMax = tv.value;
                    minmaxTime = timestamp;
                }

                if (dataMin == null || tv.value < dataMin) {

                    dataMin = tv.value;
                    minmaxTime = timestamp;
                }
            }
        }

        logger.info("Recalculated in {}ms", (clock.instant().toEpochMilli() - startTime));

        // VT: NOTE: squid:S2629 - give me a break, this will happen once in more than three hours

        logger.info("New minmaxTime set to + {}", Interval.toTimeInterval(clock.instant().toEpochMilli() - minmaxTime));
    }
}
