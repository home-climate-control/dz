package net.sf.dz3.view.swing.thermostat;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import net.sf.dz3.controller.DataSet;
import net.sf.jukebox.datastream.signal.model.DataSample;

/**
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2020
 * @deprecated Use {@link Chart2016} instead.
 */
@Deprecated
public class Chart2009 extends AbstractChart {

    private static final long serialVersionUID = -8138341010404232436L;

    public Chart2009(long chartLengthMillis) {

        super(chartLengthMillis);
    }

    @Override
    public synchronized void consume(DataSample<TintedValueAndSetpoint> signal) {

        assert(signal != null);
        assert(signal.sample != null);

        String channel = signal.sourceName;
        DataSet<TintedValue> ds = channel2dsValue.get(channel);

        if (ds == null) {

            ds = new DataSet<TintedValue>(chartLengthMillis);
            channel2dsValue.put(channel, ds);
        }

        ds.record(signal.timestamp, signal.sample);
        adjustVerticalLimits(signal.timestamp, signal.sample.value, signal.sample.setpoint);

        repaint();
    }

    @Override
    protected void paintChart(Graphics2D g2d, Dimension boundary, Insets insets,
            long now, double x_scale, long x_offset, double y_scale, double y_offset,
            String channel, DataSet<TintedValue> dsValues, DataSet<Double> dsSetpoints) {

        DataSet<TintedValue> sparceSet = spaceOut(dsValues, boundary.width);

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

                if (time_now - time_trailer <= DEAD_TIMEOUT) {

                } else {

                    if (!dead) {

                        dead = true;
                    }

                    // Paint the horizontal line in dead color
                    // and skew the x0 so the next part will be
                    // painted vertical

                    Color startColor = signal2color(trailer.tint - 1, SIGNAL_COLOR_LOW, SIGNAL_COLOR_HIGH);
                    Color endColor = getBackground();

                    drawGradientLine(g2d, x0, y0, x1, y0, startColor, endColor, cursor.emphasize);

                    x0 = x1;
                }

                if (dead) {
                    dead = false;
                }

                Color startColor = signal2color(trailer.tint - 1, SIGNAL_COLOR_LOW, SIGNAL_COLOR_HIGH);
                Color endColor = signal2color(cursor.tint - 1, SIGNAL_COLOR_LOW, SIGNAL_COLOR_HIGH);

                drawGradientLine(g2d, x0, y0, x1, y1, startColor, endColor, cursor.emphasize);
            }

            time_trailer = time_now;

            trailer = new TintedValue(cursor.value, cursor.tint, cursor.emphasize);
        }

        if (time_trailer != null && now - time_trailer > DEAD_TIMEOUT) {

            // There's a gap on the right, let's fill it

            double x0 = (time_trailer - x_offset) * x_scale
                    + insets.left;
            double x1 = (now - x_offset) * x_scale + insets.left;
            double y = (y_offset - trailer.value) * y_scale + insets.top;

            Color startColor = signal2color(trailer.tint - 1, SIGNAL_COLOR_LOW, SIGNAL_COLOR_HIGH);
            Color endColor = getBackground();

            drawGradientLine(g2d, x0, y, x1, y, startColor, endColor, false);
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
        int emphasizeAccumulator = 0;

        for (Iterator<Entry<Long, TintedValue>> i = buffer.entrySet().iterator(); i.hasNext(); ) {

            Entry<Long, TintedValue> entry = i.next();
            TintedValue signal = entry.getValue();

            valueAccumulator += signal.value;
            tintAccumulator += signal.tint;
            emphasizeAccumulator += signal.emphasize ? 1 : 0;

            i.remove();
        }

        return new TintedValue(valueAccumulator / size, tintAccumulator / size, emphasizeAccumulator > 0);
    }

    @Override
    protected void checkWidth(Dimension boundary) {
        // Does nothing here
    }
}
