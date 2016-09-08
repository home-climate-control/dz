package net.sf.dz3.view.swing.thermostat;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import net.sf.dz3.controller.DataSet;
import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.util.Interval;

/**
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2016
 */
public class Chart extends AbstractChart {

    private static final long serialVersionUID = -8138341010404232436L;

    private transient final SortedMap<String, DataSet<TintedValue>> channel2ds = new TreeMap<String, DataSet<TintedValue>>();

    /**
     * Timestamp on {@link #dataMin} or {@link #dataMax}, whichever is younger.
     * 
     * @see #adjustVerticalLimits(double)
     */
    private Long minmaxTime = null;
    
    public Chart(long chartLengthMillis) {
        
        super(chartLengthMillis);
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
        adjustVerticalLimits(signal.timestamp, signal.sample.value);

        repaint();
    }

    /**
     * Adjust the vertical limits, if necessary.
     * 
     * @param timestamp Value timestamp.
     * @param value Incoming data element.
     * 
     * @see #dataMax
     * @see #dataMin
     */
    private void adjustVerticalLimits(long timestamp, double value) {
        
        if ((minmaxTime != null) && (timestamp - minmaxTime > chartLengthMillis * minmaxOverhead)) {
            
            logger.info("minmax too old (" + Interval.toTimeInterval(timestamp - minmaxTime) + "), recalculating");

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
    }

    /**
     * Calculate {@link #dataMin} and {@link #dataMax} based on all values available in {@link #channel2ds}.
     */
    private synchronized void recalculateVerticalLimits() {
        
        long startTime = System.currentTimeMillis();
        
        dataMin = null;
        dataMax = null;
        
        for (Iterator<DataSet<TintedValue>> i = channel2ds.values().iterator(); i.hasNext(); ) {
            
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
        
        logger.info("Recalculated in " + (System.currentTimeMillis() - startTime) + "ms");
        logger.info("New minmaxTime set to + " + Interval.toTimeInterval(System.currentTimeMillis() - minmaxTime));
    }

    @Override
    protected void paintCharts(Graphics2D g2d, Dimension boundary, Insets insets, long now, double x_scale, long x_offset, double y_scale, double y_offset) {

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (Iterator<Entry<String, DataSet<TintedValue>>> i = channel2ds.entrySet().iterator(); i.hasNext(); ) {

            // VT: FIXME: Implement depth ordering

            Entry<String, DataSet<TintedValue>> entry = i.next();
            String channel = entry.getKey();
            DataSet<TintedValue> ds = entry.getValue();

            paintChart(g2d, boundary, insets, now, x_scale, x_offset, y_scale, y_offset, channel, ds);
        }
    }

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

                    Color startColor = signal2color(trailer.tint - 1, SIGNAL_COLOR_LOW, SIGNAL_COLOR_HIGH);
                    Color endColor = getBackground();

                    drawGradientLine(g2d, x0, y0, x1, y0, startColor, endColor);

                    x0 = x1;
                }

                if (dead) {
                    dead = false;
                }

                Color startColor = signal2color(trailer.tint - 1, SIGNAL_COLOR_LOW, SIGNAL_COLOR_HIGH);
                Color endColor = signal2color(cursor.tint - 1, SIGNAL_COLOR_LOW, SIGNAL_COLOR_HIGH);

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

            Color startColor = signal2color(trailer.tint - 1, SIGNAL_COLOR_LOW, SIGNAL_COLOR_HIGH);
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

    @Override
    protected boolean isDataAvailable() {

        if (channel2ds.isEmpty() || dataMax == null || dataMin == null) {

            // No data consumed yet
            return false;
        }
        
        return true;

    }
}
