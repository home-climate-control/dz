package net.sf.dz3r.signal.hvac;

import net.sf.dz3r.device.actuator.HvacDevice;

import java.time.Duration;

/**
 * Hierarchy base for the status of any {@link HvacDevice}.
 *
 * @see net.sf.dz3r.signal.health.HvacDeviceStatus
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2023
 */
public abstract class HvacDeviceStatus {
    public enum Kind {
        REQUESTED,
        ACTUAL
    }

    public final Kind kind;
    public final HvacCommand requested;

    /**
     * Duration since the device turned on this time, {@code null} if it is currently off.
     */
    public final Duration uptime;

    protected HvacDeviceStatus(Kind kind, HvacCommand requested, Duration uptime) {
        this.kind = kind;
        this.requested = requested;
        this.uptime = uptime;
    }

    @Override
    public String toString() {
        return "{kind=" + kind + ", requested=" + requested + ", uptime=" + uptime + "}";
    }
}
