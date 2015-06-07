package net.sf.dz3.view.swing.thermostat;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.JPanel;

import net.sf.dz3.controller.DataSet;
import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.datastream.signal.model.DataSink;

import org.apache.log4j.Logger;

/**
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2012
 */
public class Chart extends JPanel implements DataSink<TintedValue> {

    private static final long serialVersionUID = 6232379779841821973L;
    private final Logger logger = Logger.getLogger(getClass());

    private final SortedMap<String, DataSet<TintedValue>> channel2ds = new TreeMap<String, DataSet<TintedValue>>();

    /**
     * Grid color.
     * 
     * Default is dark gray.
     */
    private Color gridColor = Color.darkGray;

    /**
     * Chart length, in milliseconds.
     */
    private long chartLengthMillis;

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
    private long deadTimeout = 1000 * 60;

    /**
     * Horizontal grid spacing.
     * 
     * Vertical grid lines will be painted every <code>timeSpacing</code>
     * milliseconds. Default is 30 minutes.
     */
    private long timeSpacing = 1000 * 60 * 30;

    /**
     * Vertical grid spacing.
     * 
     * Horizontal grid lines will be painted every <code>valueSpacing</code>
     * units. Default is 1.0.
     */
    private double valueSpacing = 1.0;

    /**
     * How much space to leave between the chart and the edge.
     */
    private double padding = 0.2;

    /**
     * Maximum known data value.
     */
    private Double dataMax = null;

    /**
     * Minimum known data value.
     */
    private Double dataMin = null;

    public Chart() {

        // 30 minutes

        this(1000 * 60 * 30);
    }

    public Chart(long chartLengthMillis) {

        assert(chartLengthMillis > 1000 * 10);

        this.chartLengthMillis = chartLengthMillis;
    }

    public void setGridColor(Color gridColor) {

        assert(gridColor != null);

        this.gridColor = gridColor;
    }

    @Override
    public synchronized void consume(DataSample<TintedValue> signal) {

        assert(signal != null);
        assert(signal.sample != null);

        String channel = signal.sourceName;
        DataSet<TintedValue> ds = channel2ds.get(channel);

        if (ds == null) {

            // 3 hours
            ds = new DataSet<TintedValue>(1000 * 60 * 60 * 3);
            channel2ds.put(channel, ds);
        }

        ds.record(signal.timestamp, signal.sample);
        adjustVerticalLimits(signal.sample.value);

        repaint();
    }

    /**
     * Adjust the vertical limits, if necessary.
     * 
     * @param value Incoming data element.
     * 
     * @see #dataMax
     * @see #dataMin
     */
    private void adjustVerticalLimits(double value) {

        if (dataMax == null || value > dataMax) {
            dataMax = value;
        }

        if (dataMin == null || value < dataMin) {
            dataMin = value;
        }
    }

    @Override
    public synchronized void paintComponent(Graphics g) {

        // VT: FIXME: Consider replacing this with a Marker - careful, though, this is a time sensitive path
        long startTime = System.currentTimeMillis();

        // Draw background
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        Dimension boundary = getSize();
        Insets insets = getInsets();

        paintBackground(g2d, boundary, insets);

        long now = System.currentTimeMillis();
        double x_scale = (double) (boundary.width - insets.left - insets.right) / (double) chartLengthMillis;
        long x_offset = now - chartLengthMillis;

        paintTimeGrid(g2d, boundary, insets, now, x_scale, x_offset);

        if (channel2ds.isEmpty() || dataMax == null || dataMin == null) {

            // No data consumed yet
            return;
        }

        double y_scale = (double) (boundary.height - insets.bottom - insets.top) / (dataMax - dataMin + padding * 2);
        double y_offset = dataMax + padding;

        paintValueGrid(g2d, boundary, insets, now, x_scale, x_offset, y_scale, y_offset);

        paintChart(g2d, boundary, insets, now, x_scale, x_offset, y_scale, y_offset);

        logger.info("Painted in " + (System.currentTimeMillis() - startTime) + "ms");
    }

    private void paintBackground(Graphics2D g2d, Dimension boundary,
            Insets insets) {

        g2d.setPaint(getBackground());

        Rectangle2D.Double background = new Rectangle2D.Double(
                insets.left, insets.top,
                boundary.width - insets.right - insets.left, boundary.height - insets.bottom - insets.top);

        g2d.fill(background);
    }

    private void paintTimeGrid(Graphics2D g2d, Dimension boundary, Insets insets, long now, double x_scale, long x_offset) {

        BasicStroke originalStroke = (BasicStroke) g2d.getStroke();

        g2d.setPaint(gridColor);

        float[] gridDash = { 2, 2 };

        BasicStroke gridStroke = new BasicStroke(
                originalStroke.getLineWidth(), originalStroke.getEndCap(),
                originalStroke.getLineJoin(),
                originalStroke.getMiterLimit(), gridDash,
                originalStroke.getDashPhase());

        g2d.setStroke(gridStroke);

        for (long timeOffset = now - timeSpacing; timeOffset > now - chartLengthMillis; timeOffset -= timeSpacing) {

            double gridX = (timeOffset - x_offset) * x_scale + insets.left;

            drawGradientLine(g2d, gridX, insets.top, gridX, boundary.height - insets.bottom - 1,
                    getBackground(), Color.GRAY.darker().darker());
        }

        g2d.setStroke(originalStroke);
    }

    private void paintValueGrid(Graphics2D g2d, Dimension boundary, Insets insets, long now, double x_scale, long x_offset, double y_scale, double y_offset) {

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

        double gridY = y_offset * y_scale + insets.top;

        Line2D gridLine = new Line2D.Double(insets.left, gridY, boundary.width
                - insets.right - 1, gridY);

        g2d.draw(gridLine);

        // All the rest of the grid lines get painted with a dashed line

        g2d.setStroke(gridStroke);

        double valueOffset = 0;
        double halfWidth = ((double) (boundary.width - insets.right - 1)) / 2d;

        for (valueOffset = valueSpacing; valueOffset < dataMax + padding; valueOffset += valueSpacing) {

            gridY = (y_offset - valueOffset) * y_scale + insets.top;

            //			gridLine = new Line2D.Double(insets.left, gridY, boundary.width - insets.right - 1, gridY);
            //			g2d.draw(gridLine);

            drawGradientLine(g2d,
                    insets.left, gridY,
                    halfWidth, gridY,
                    Color.GRAY.darker().darker(), getBackground());

            drawGradientLine(g2d,
                    halfWidth, gridY,
                    boundary.width - insets.right - 1, gridY,
                    getBackground(), Color.GRAY.darker().darker());
        }

        for (valueOffset = -valueSpacing; valueOffset > dataMin - padding; valueOffset -= valueSpacing) {

            gridY = (y_offset - valueOffset) * y_scale + insets.top;

            //			gridLine = new Line2D.Double(insets.left, gridY, boundary.width - insets.right - 1, gridY);
            //			g2d.draw(gridLine);

            drawGradientLine(g2d,
                    insets.left, gridY,
                    halfWidth, gridY,
                    getBackground(), Color.GRAY.darker().darker());

            drawGradientLine(g2d,
                    halfWidth, gridY,
                    boundary.width - insets.right - 1, gridY,
                    getBackground(), Color.GRAY.darker().darker());
        }

        g2d.setStroke(originalStroke);
    }

    private void paintChart(Graphics2D g2d, Dimension boundary, Insets insets, long now, double x_scale, long x_offset, double y_scale, double y_offset) {

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (Iterator<Entry<String, DataSet<TintedValue>>> i = channel2ds.entrySet().iterator(); i.hasNext(); ) {

            // VT: FIXME: Implement depth ordering

            Entry<String, DataSet<TintedValue>> entry = i.next();
            String channel = entry.getKey();
            DataSet<TintedValue> ds = entry.getValue();

            paintChart(g2d, boundary, insets, now, x_scale, x_offset, y_scale, y_offset, channel, ds);
        }
    }

    final static Color low = Color.GREEN;
    final static Color high = Color.RED;

    private void paintChart(Graphics2D g2d, Dimension boundary, Insets insets,
            long now, double x_scale, long x_offset, double y_scale, double y_offset,
            String channel, DataSet<TintedValue> ds) {

        DataSet<TintedValue> sparceSet = spaceOut(ds, boundary.width);

        Long time_trailer = null;
        TintedValue trailer = null;

        // Flag to reduce the color changes
        boolean dead = false;

        for (Iterator<Long> di = sparceSet.iterator(); di.hasNext();) {

            long time_now = di.next();
            TintedValue cursor = sparceSet.get(time_now);

            if (time_trailer != null) {

                double x0 = (time_trailer - x_offset) * x_scale
                        + insets.left;
                double y0 = (y_offset - trailer.value) * y_scale
                        + insets.top;

                double x1 = (time_now - x_offset) * x_scale
                        + insets.left;
                double y1 = (y_offset - cursor.value) * y_scale
                        + insets.top;

                // Decide whether the line is alive or dead

                if (time_now - time_trailer <= deadTimeout) {
                    
                } else {

                    if (!dead) {

                        dead = true;
                    }

                    // Paint the horizontal line in dead color
                    // and skew the x0 so the next part will be
                    // painted vertical

                    Color startColor = signal2color(trailer.tint - 1, low, high);
                    Color endColor = getBackground();

                    drawGradientLine(g2d, x0, y0, x1, y0, startColor, endColor);

                    x0 = x1;
                }

                if (dead) {
                    dead = false;
                }

                Color startColor = signal2color(trailer.tint - 1, low, high);
                Color endColor = signal2color(cursor.tint - 1, low, high);

                drawGradientLine(g2d, x0, y0, x1, y1, startColor, endColor);
            }

            time_trailer = time_now;

            trailer = new TintedValue(cursor.value, cursor.tint);
        }

        if (time_trailer != null && now - time_trailer > deadTimeout) {

            // There's a gap on the right, let's fill it

            double x0 = (time_trailer - x_offset) * x_scale
                    + insets.left;
            double x1 = (now - x_offset) * x_scale + insets.left;
            double y = (y_offset - trailer.value) * y_scale + insets.top;

            Color startColor = signal2color(trailer.tint - 1, low, high);
            Color endColor = getBackground();

            drawGradientLine(g2d, x0, y, x1, y, startColor, endColor);
        }

        // Store the values so the readings can be displayed
        // over the curves

        //		if (value_trailer != null) {
        //			values.put(name, value_trailer);
        //		}
    }

    private DataSet<TintedValue> spaceOut(DataSet<TintedValue> source, int width) {

        DataSet<TintedValue> target = new DataSet<TintedValue>(source.getExpirationInterval()); 
        long step = chartLengthMillis / width;

        logger.info("Source: " + source.size() + " samples");
        logger.info("ms per pixel: " + step);

        step *= 2;

        SortedMap<Long, TintedValue> overflow = new TreeMap<Long, TintedValue>();
        SortedMap<Long, TintedValue> buffer = new TreeMap<Long, TintedValue>();
        Long cutoff = null;

        for (Iterator<Long> i = source.iterator(); i.hasNext();) {

            long timestamp = i.next();
            TintedValue value = source.get(timestamp);

            if (cutoff == null) {

                cutoff = timestamp;
                buffer.put(timestamp, value);

                continue;
            }

            if (!overflow.isEmpty()) {

                Long key = overflow.firstKey();
                buffer.put(key, overflow.get(key));
                overflow.clear();

                cutoff = key;
            }

            if (timestamp > cutoff + step) {

                overflow.put(timestamp, value);
                Long last = buffer.lastKey();
                target.record(last, spaceOut(buffer));

                continue;
            }

            buffer.put(timestamp, value);
        }

        logger.info("Target: " + target.size() + " samples");

        return target;
    }

    private TintedValue spaceOut(SortedMap<Long, TintedValue> buffer) {

        int size = buffer.size();

        assert(size > 0);

        double valueAccumulator = 0;
        double tintAccumulator = 0;

        for (Iterator<Entry<Long, TintedValue>> i = buffer.entrySet().iterator(); i.hasNext(); ) {

            Entry<Long, TintedValue> entry = i.next();
            TintedValue signal = entry.getValue();

            valueAccumulator += signal.value;
            tintAccumulator += signal.tint;

            i.remove();
        }

        return new TintedValue(valueAccumulator / size, tintAccumulator / size);
    }

    /**
     * Draw the gradient line between given points and given colors.
     */
    private void drawGradientLine(Graphics2D g2d, double x0, double y0, double x1, double y1, Color startColor, Color endColor) {

        GradientPaint gp = new GradientPaint(
                (int) x0, (int) y0, startColor,
                (int) x1, (int) y1, endColor);
        Line2D line = new Line2D.Double(x0, y0, x1, y1);

        g2d.setPaint(gp);
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
    private Color signal2color(double signal, Color low, Color high) {

        signal = signal > 1 ? 1: signal;
        signal = signal < -1 ? -1 : signal;
        signal = (signal + 1) / 2;

        int index = (int) (signal * 255);

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

    private static class RGB2HSB {

        public final int rgb;
        public final float hsb[];

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

        rgb2hsb[offset] = new RGB2HSB(rgb, Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null));

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
}
