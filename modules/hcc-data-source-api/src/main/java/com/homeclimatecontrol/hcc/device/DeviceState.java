package com.homeclimatecontrol.hcc.device;

import net.sf.dz3r.common.HCCObjects;

/**
 * Device state abstraction.
 *
 * @param <T> Payload type.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2023
 */
public class DeviceState<T> {

    public final String id;

    /**
     * {@code true} if the device was reported as available, {@code false} if not, {@code null} if it is unknown.
     */
    public final Boolean available;

    /**
     * Requested device state.
     */
    public final T requested;

    /**
     * Actual device state.
     */
    public final T actual;

    /**
     * Current value of send queue depth for this device, {@code null} if unknown.
     */
    public final Integer queueDepth;

    public DeviceState(
            String id,
            Boolean available,
            T requested,
            T actual,
            Integer queueDepth
    ) {
        this.id = HCCObjects.requireNonNull(id, "id can't be null");
        this.available = available;
        this.requested = requested;
        this.actual = actual;
        this.queueDepth = queueDepth;
    }

    @Override
    public final String toString() {
        return "{available=" + available + ", requested=" + requested + ", actual=" + actual + ", queueDepth=" + queueDepth + "}";
    }
}
