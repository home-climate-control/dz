package net.sf.dz3.view.swing.thermostat;

/**
 * Intended to hold a data sample for the {@link AbstractChart variable color chart}.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2018
 */
public class TintedValue {

    /**
     * Value Y coordinate on the chart.
     */
    public final double value;

    /**
     * The tint.
     *
     * Allowable values are {@code -1d} to {@code 1d}. Translates into the point color on the chart,
     * {@code -1d} and {@code 1d} being the far extremes of the color gradient.
     */
    public final double tint;

    /**
     * {@code true} if this particular point on the chart needs to be emphasized.
     */
    public final boolean emphasize;

    /**
     * Setpoint Y coordinate on the chart.
     *
     * VT: FIXME: Makes little sense in the context of a single value, a prime candidate for optimization.
     */
    public final double setpoint;

    public TintedValue(double value, double tint, boolean emphasize, double setpoint) {

        this.value = value;
        this.tint = tint;
        this.emphasize = emphasize;
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
