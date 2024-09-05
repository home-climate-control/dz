package net.sf.dz3r.signal.hvac;

import com.homeclimatecontrol.hcc.signal.hvac.HvacCommand;
import net.sf.dz3r.device.DeviceState;
import net.sf.dz3r.device.actuator.HvacDevice;

import java.time.Duration;

/**
 * Hierarchy base for the status of any {@link HvacDevice}.
 *
 * @param command The state requested by the last incoming command that resulted in this update.
 * @param uptime  Duration since the device turned on this time, {@code null} if it is currently off.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2024
 */
public record HvacDeviceStatus<T>(
        HvacCommand command,
        Duration uptime,
        DeviceState<T> deviceState) {
}
