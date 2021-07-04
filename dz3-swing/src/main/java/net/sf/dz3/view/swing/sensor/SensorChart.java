package net.sf.dz3.view.swing.sensor;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import net.sf.dz3.view.swing.AbstractChart;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.time.Clock;

public class SensorChart extends AbstractChart<Double> {

    protected SensorChart(Clock clock, long chartLengthMillis) {
        super(clock, chartLengthMillis);
    }

    @Override
    public void consume(DataSample<Double> signal) {

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
}
