package net.sf.dz3r.signal;

import java.time.Instant;

/**
 * Base interface for all the signals in the system.
 *
 * @param <T> Signal value type.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2021
 */
public class Signal<T> {

    public enum Status {
        OK,
        FAILURE_PARTIAL,
        FAILURE_TOTAL
    }

    public final Instant timestamp;
    private final T value;
    public final Status status;
    public final Throwable error;

    /**
     * Construct a non-error signal.
     *
     * @param timestamp Signal timestamp.
     * @param value Signal value.
     */
    public Signal(Instant timestamp, T value) {
        this(timestamp, value, Status.OK, null);
    }

    public Signal(Instant timestamp, T value, Status status, Throwable error) {

        if (timestamp == null) {
            throw new IllegalArgumentException("timestamp can't be null");
        }

        if (value == null && status == Status.OK) {
            throw new IllegalArgumentException("null value doesn't make sense for status OK");
        }

        this.timestamp = timestamp;
        this.value = value;

        this.status = status;
        this.error = error;
    }

    /**
     * Get the value.
     *
     * @return The value. Careful, this may be non-null even if the signal is {@link #isError()}.
     */
    public T getValue() {

        return value;
    }

    /**
     * Find out if the signal source is OK.
     *
     * Note that this may return {@code false} even if the value is present - for the case of partial failure.
     *
     * @return {@code true} if there are no failures, otherwise {@code false}.
     */
    public boolean isOK() {
        return status == Status.OK;
    }

    /**
     * Find out if the signal source is in error.
     *
     * Note that this may return {@code false} even if {@link #isOK()} returns {@code false} - the failure may be
     * partial and not yet impact the system operation.
     *
     * @return {@code true} if the failure is {@link Status#FAILURE_TOTAL}.
     */
    public boolean isError() {
        return status == Status.FAILURE_TOTAL;
    }

    public Status getStatus() {
        return status;
    }

    public Throwable getError() {
        return error;
    }

    @Override
    public String toString() {
        return "@" + timestamp + "=" + value + ", isOK=" + isOK() + ", isError=" + isError();
    }
}
