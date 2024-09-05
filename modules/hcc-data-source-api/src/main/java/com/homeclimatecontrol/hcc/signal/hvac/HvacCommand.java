package com.homeclimatecontrol.hcc.signal.hvac;

import com.homeclimatecontrol.hcc.model.HvacMode;

/**
 * Signal emitted by the {@link net.sf.dz3r.model.UnitController}
 * and other entities that may want to control a HVAC device.
 * <p>
 * {@code null} values indicate that no change for that parameter is desired.
 * It is assumed that the producer of this object knows what they're doing and assumes full responsibility
 * for the device state.
 * <p>
 * An instance with all {@code null} values indicates an initial state.
 *
 * @param mode     Nullable operating mode.
 * @param demand   Nullable demand.
 * @param fanSpeed Nullable fan speed.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2024
 */
public record HvacCommand(
        HvacMode mode,
        Double demand,
        Double fanSpeed) {
}
