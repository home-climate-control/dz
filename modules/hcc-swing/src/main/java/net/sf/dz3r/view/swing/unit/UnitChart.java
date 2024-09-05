package net.sf.dz3r.view.swing.unit;

import com.homeclimatecontrol.hcc.signal.Signal;
import com.homeclimatecontrol.hcc.signal.hvac.HvacDeviceStatus;
import net.sf.dz3r.view.swing.AbstractChart;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class UnitChart extends AbstractChart<HvacDeviceStatus, Void> {
    /**
     * Defines how wide the unit chart is the moment it turns on.
     *
     * Runtimes less than 10 minutes are a sure sign of the unit being oversized,
     * and long runtimes are quite variable.
     *
     * Also, defines how much time is added to the chart when it is about to be filled up.
     */
    private static final Duration timeSpanIncrement = Duration.of(15, ChronoUnit.MINUTES);

    protected UnitChart(Clock clock) {
        super(clock, timeSpanIncrement.toMillis(), false);
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
        logger.debug("FIXME: paintCharts()");
    }

    @Override
    protected boolean update(Signal<HvacDeviceStatus, Void> signal) {
        return true;
    }
}
