package net.sf.dz3.view.swing.thermostat;

/**
 * Intended to hold a data sample for the {@link AbstractChart variable color chart}.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2020
 */
public class TintedValueAndSetpoint extends TintedValue {

    /**
     * Setpoint Y coordinate on the chart.
     *
     * VT: FIXME: Makes little sense in the context of a single value, a prime candidate for optimization.
     */
    public final double setpoint;

    public TintedValueAndSetpoint(double value, double tint, boolean emphasize, double setpoint) {

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
