package net.sf.dz3r.view.swing.zone;

import net.sf.dz3r.signal.hvac.EconomizerStatus;

/**
 * {@link AbstractZoneChart} data point wrapper.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class ZoneChartDataPoint {

    public final ThermostatTintedValue tintedValue;

    /**
     * Setpoint Y coordinate on the chart.
     *
     * VT: FIXME: Makes little sense in the context of a single value, a prime candidate for optimization.
     */
    public final double setpoint;

    public final EconomizerStatus economizerStatus;

    public ZoneChartDataPoint(
            ThermostatTintedValue tintedValue,
            double setpoint,
            EconomizerStatus economizerStatus) {

        this.tintedValue = tintedValue;
        this.setpoint = setpoint;
        this.economizerStatus = economizerStatus;
    }

    @Override
    public String toString() {

        return "{tintedValue=" + tintedValue + ",setpoint=" + setpoint + ",economizer=" + economizerStatus + "}";
    }
}
