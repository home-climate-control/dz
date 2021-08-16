package net.sf.dz3r.signal;

import net.sf.dz3r.model.HvacMode;

/**
 * Signal emitted by the {@link net.sf.dz3r.model.UnitController}
 * and other entities that may want to control a HVAC device.
 *
 * {@code null} values indicate that no change for that parameter is desired.
 * It is assumed that the producer of this object knows what they're doing and assumes full responsibility
 * for the device state.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class HvacCommand {

    public final Double demand;
    public final Double fanSpeed;
    public final HvacMode mode;


    /**
     * Create an instance.
     *
     * An instance with all {@code null} values indicates an initial state.
     *
     * @param mode Nullable operating mode.
     * @param demand Nullable demand.
     * @param fanSpeed Nullable fan speed.
     */
    public HvacCommand( HvacMode mode, Double demand, Double fanSpeed) {

        this.mode = mode;
        this.demand = demand;
        this.fanSpeed = fanSpeed;
    }

    @Override
    public String toString() {
        return "{mode=" + mode + ", demand=" + demand + ", fanSpeed=" + fanSpeed + "}";
    }
}
