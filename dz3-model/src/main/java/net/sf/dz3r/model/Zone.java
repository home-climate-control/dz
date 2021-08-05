package net.sf.dz3r.model;

import net.sf.dz3r.device.Addressable;

/**
 * Climate controlled zone.
 *
 * Formerly a {@link net.sf.dz3.device.model.Thermostat}.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class Zone implements Addressable<String> {

    private final String name;
    private Range<Double> setpointRange;
    private ZoneState state;

    /**
     * Create a zone with a default 10C..40C setpoint range.
     *
     * @param name Zone name.
     */
    public Zone(String name) {
        this(name, new Range<>(10d, 40d));
    }

    /**
     * Create a zone with a custom setpoint range.
     *
     * @param name Zone name.
     * @param setpointRange Setpoint range for this zone.
     */
    public Zone(String name, Range<Double> setpointRange) {
        this.name = name;
        this.setpointRange = setpointRange;
    }

    /**
     * Get the human readable zone name.
     *
     * @return Zone name.
     */
    @Override
    public String getAddress() {
        return name;
    }

    private void configurationChanged() {
        // Do nothing yet
    }
}
