package net.sf.dz3r.view.swing.zone;

/**
 * {@link AbstractZoneChart} data point wrapper.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class ZoneChartDataPoint {

    public final TintedValue tintedValue;

    /**
     * Setpoint Y coordinate on the chart.
     *
     * VT: FIXME: Makes little sense in the context of a single value, a prime candidate for optimization.
     */
    public final double setpoint;

    public ZoneChartDataPoint(TintedValue tintedValue, double setpoint) {

        this.tintedValue = tintedValue;
        this.setpoint = setpoint;
    }

    @Override
    public String toString() {

        return "{tintedValue=" + tintedValue + ",setpoint=" + setpoint + "}";
    }
}
