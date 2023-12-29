package net.sf.dz3r.signal.hvac;

import net.sf.dz3r.device.DeviceState;
import net.sf.dz3r.device.actuator.HvacDevice;

import java.time.Duration;

/**
 * Hierarchy base for the status of any {@link HvacDevice}.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2023
 */
public class HvacDeviceStatus<T> {

    /**
     * The state requested by the last incoming command that resulted in this update.
     */
    public final HvacCommand command;

    /**
     * Duration since the device turned on this time, {@code null} if it is currently off.
     */
    public final Duration uptime;

    public final DeviceState<T> deviceState;

    public HvacDeviceStatus(HvacCommand command, Duration uptime, DeviceState<T> deviceState) {
        this.command = command;
        this.uptime = uptime;
        this.deviceState = deviceState;
    }

    @Override
    public String toString() {
        return "{command=" + command + ", uptime=" + uptime + ", deviceState=" + deviceState + "}";
    }
}
