package com.homeclimatecontrol.hcc.signal.hvac;

/**
 * Calling status.
 * <p>
 * This object defines the actual thermostat or economizer status in real time.
 *
 * @param sample Raw component of the hysteresis controller. Here for instrumentation purposes only.
 * @param demand Generated demand.
 * @param calling {@code true} if the zone this status is reported from is calling for heat or cool.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2024
 */
public record CallingStatus(
        Double sample,
        double demand,
        boolean calling) {
}
