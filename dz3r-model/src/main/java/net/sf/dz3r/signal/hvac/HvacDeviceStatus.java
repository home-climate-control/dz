package net.sf.dz3r.signal.hvac;

import java.time.Duration;

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
