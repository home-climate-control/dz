package net.sf.dz3r.device.onewire.event;

import java.time.Instant;

/**
 * Base class for all error events that can be emitted by a 1-Wire network.
 *
 * @param <P> Payload type.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2000-2021
 */
public class OneWireNetworkErrorEvent<P> extends OneWireNetworkEvent {

    /**
     * Whether this error is transient. {@code null} means unknown.
     */
    public final Boolean isTransient;
    /**
     * Whether this error is fatal. {@code null} means unknown.
     */
    public final Boolean isFatal;

    /**
     * The error cause, must be present.
     */
    public final Throwable error;

    public OneWireNetworkErrorEvent(Instant timestamp, Boolean isTransient, Boolean isFatal, Throwable error) {
        super(timestamp);

        if (error == null) {
            throw new IllegalArgumentException("error can't be null");
        }

        this.isTransient = isTransient;
        this.isFatal = isFatal;
        this.error = error;
    }

    @Override
    public String toString() {
        return "{1-Wire error timestamp=" + timestamp
                + ", transient=" + isTransient
                + ", fatal=" + isFatal
                + ", error=" + error.getClass().getName() + "(" + error.getMessage() + ")}";
    }
}
