package net.sf.dz3r.device.driver.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all error events that can be emitted by a device driver network.
 *
 * @param <P> Payload type.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2000-2021
 */
public class DriverNetworkErrorEvent<P> extends DriverNetworkEvent {

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

    public DriverNetworkErrorEvent(Instant timestamp, UUID correlationId, Boolean isTransient, Boolean isFatal, Throwable error) {
        super(timestamp, correlationId);

        if (error == null) {
            throw new IllegalArgumentException("error can't be null");
        }

        this.isTransient = isTransient;
        this.isFatal = isFatal;
        this.error = error;
    }

    @Override
    public String toString() {
        return "{Error timestamp=" + timestamp
                + ", correlationId=" + correlationId
                + ", transient=" + isTransient
                + ", fatal=" + isFatal
                + ", error=" + error.getClass().getName() + "(" + error.getMessage() + ")}";
    }
}
