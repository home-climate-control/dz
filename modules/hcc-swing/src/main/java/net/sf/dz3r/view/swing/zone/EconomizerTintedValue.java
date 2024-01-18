package net.sf.dz3r.view.swing.zone;

/**
 * Intended to hold an economizer data sample for the {@link AbstractZoneChart variable color chart}.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 *
 * @see ThermostatTintedValue
 */
public class EconomizerTintedValue {

    /**
     * Ambient temperature.
     */
    public final double ambient;

    /**
     * Control signal.
     */
    public final double signal;

    public EconomizerTintedValue(double ambient, double signal) {

        this.ambient = ambient;
        this.signal = signal;
    }

    @Override
    public String toString() {

        return "{ambient=" + ambient + ", signal=" + signal + "}";
    }
}
