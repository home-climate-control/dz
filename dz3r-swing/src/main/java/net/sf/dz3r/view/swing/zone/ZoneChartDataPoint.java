package net.sf.dz3r.view.swing.zone;

/**
 * {@link AbstractZoneChart} data point wrapper.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class ZoneChartDataPoint extends TintedValue {

    /**
     * Setpoint Y coordinate on the chart.
     *
     * VT: FIXME: Makes little sense in the context of a single value, a prime candidate for optimization.
     */
    public final double setpoint;

    public ZoneChartDataPoint(double value, double tint, boolean emphasize, double setpoint) {

        super(value, tint, emphasize);

        this.setpoint = setpoint;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();

        sb.append("TintedValue(").append(value).append(", ").append(tint).append(", ").append(emphasize).append(", ").append(setpoint);
        sb.append(")");

        return sb.toString();
    }
}
