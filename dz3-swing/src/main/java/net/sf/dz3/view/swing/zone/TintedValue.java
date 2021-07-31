package net.sf.dz3.view.swing.zone;

/**
 * Intended to hold a data sample for the {@link AbstractZoneChart variable color chart}.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2020
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

    public TintedValue(double value, double tint, boolean emphasize) {

        this.value = value;
        this.tint = tint;
        this.emphasize = emphasize;
    }
}
