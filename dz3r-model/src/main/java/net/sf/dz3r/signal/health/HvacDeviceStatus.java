package net.sf.dz3r.signal.health;

import net.sf.dz3r.device.actuator.HvacDevice;

import java.time.Duration;

/**
 * Status of any {@link HvacDevice}.
 *
 * Note that this object bears no failure indication, this is what {@link net.sf.dz3r.signal.Signal#status} is for.
 *
 * @param uptime Current uptime. {@code null} if the unit is off.
 *
 * @see net.sf.dz3r.signal.hvac.HvacDeviceStatus
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2023
 */
public record HvacDeviceStatus(
        Duration uptime
) {
}
