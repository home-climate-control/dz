package net.sf.dz3.view.swing.thermostat;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.dz3.controller.DataSet;
import net.sf.jukebox.datastream.signal.model.DataSample;

public class FasterChart extends AbstractChart {

    private static final long serialVersionUID = 8739949924865459025L;
    
    /**
     * Chart width in pixels for all the charts. Undefined until the first time
     * {@link #paintCharts(Graphics2D, Dimension, Insets, long, double, long, double, double)}
     * for any instance of this class is called.
     * 
     * Making it static is ugly, but gets the job done - the screen size will not change.
     */
    private static AtomicInteger globalWidth = new AtomicInteger();
    
    /**
     * Chart widhth of this instance.
     * 
     * @see #globalWidth
     * @see #paintCharts(Graphics2D, Dimension, Insets, long, double, long, double, double)
     */
    private AtomicInteger width = new AtomicInteger();

    public FasterChart(long chartLengthMillis) {

        super(chartLengthMillis);
    }

    @Override
    public synchronized void consume(DataSample<TintedValue> signal) {

        assert(signal != null);
        assert(signal.sample != null);

        String channel = signal.sourceName;
        DataSet<TintedValue> ds = channel2ds.get(channel);

        if (ds == null) {

            ds = new DataSet<TintedValue>(chartLengthMillis);
            channel2ds.put(channel, ds);
        }
        
        // If someone repaints, we'll know right away

        width.set(globalWidth.get());

        if (record(channel, ds, signal)) {

            repaint();
        }
    }

    /**
     * Record the signal, properly spacing it out.
     * 
     * @param channel Channel to use.
     * @param ds Data set to use.
     * @param signal Signal to record.
     * 
     * @return {@code true} if the component needs to be repainted.
     */
    private boolean record(String channel, DataSet<TintedValue> ds, DataSample<TintedValue> signal) {
        
        adjustVerticalLimits(signal.timestamp, signal.sample.value);

        if (width.get() == 0) {
            
            // There's nothing we can do before the width is set.
            // It's not even worth it to record the value.
            
            // Please repaint.
            return true;
        }


        return true;
    }

    @Override
    protected void paintCharts(Graphics2D g2d, Dimension boundary, Insets insets, long now, double x_scale,
            long x_offset, double y_scale, double y_offset) {
        
        if (globalWidth.getAndSet(boundary.width) == 0) {
            
            long step = chartLengthMillis / this.globalWidth.get();

            logger.info("Chart width " + globalWidth);
            logger.info("ms per pixel: " + step);
        }
        
    }
}
