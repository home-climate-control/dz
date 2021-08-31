package net.sf.dz3r.signal;

public abstract class HvacDeviceStatus {
    public enum Kind {
        REQUESTED,
        ACTUAL
    }

    public final Kind kind;
    public final HvacCommand requested;

    protected HvacDeviceStatus(Kind kind, HvacCommand requested) {
        this.kind = kind;
        this.requested = requested;
    }

    @Override
    public String toString() {
        return "{kind=" + kind + ", requested=" + requested + "}";
    }
}
