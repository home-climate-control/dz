package net.sf.dz3r.device;

/**
 * Device state abstraction.
 *
 * @param <T> Payload type.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2021
 */
public class DeviceState<T> {

    public final T requested;
    public final T actual;

    public DeviceState(T requested, T actual) {
        this.requested = requested;
        this.actual = actual;
    }

    @Override
    public final String toString() {
        return "{requested=" + requested + ", actual=" + actual + "}";
    }
}
