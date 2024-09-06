package com.homeclimatecontrol.hcc.signal;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;

/**
 * Base interface for all the signals in the system.
 *
 * @param timestamp Event time.
 * @param value Signal value. Must be present if the status is {@link Signal.Status#OK}.
 * @param payload Optional payload.
 *   {@link net.sf.dz3r.controller.ProcessController} implementations are expected to pass this signal
 *   from input to output unchanged, however, this is not enforced.
 * @param status Signal status.
 * @param error Error. Must be present if the status is not {@link Signal.Status#OK}.
 * @param <T> Signal value type.
 * @param <P> Extra payload type.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2024
 */
public record Signal<T, P>(
        Instant timestamp,
        T value,
        P payload,
        Status status,
        Throwable error
) {

    public enum Status {
        OK,
        FAILURE_PARTIAL,
        FAILURE_TOTAL
    }

    /**
     * Construct a non-error signal with no payload.
     *
     * @param timestamp Signal timestamp.
     * @param value Signal value.
     */
    public Signal(Instant timestamp, T value) {
        this(timestamp, value, null, Status.OK, null);
    }

    /**
     * Construct a non-error signal.
     *
     * @param timestamp Signal timestamp.
     * @param value Signal value.
     * @param payload Optional payload.
     */
    public Signal(Instant timestamp, T value, P payload) {
        this(timestamp, value, payload, Status.OK, null);
    }

    public Signal {

        if (timestamp == null) {
            throw new IllegalArgumentException("timestamp can't be null");
        }

        if (value == null && status != Status.FAILURE_TOTAL) {
            throw new IllegalArgumentException("null value doesn't make sense for status " + status);
        }

        if (status != Status.OK && error == null) {
            throw new IllegalArgumentException("null error doesn't make sense for status " + status);
        }

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
    @JsonIgnore
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
    @JsonIgnore
    public boolean isError() {
        return status == Status.FAILURE_TOTAL;
    }

    public Throwable getError() {
        return error;
    }

    @Override
    public String toString() {
        var result = "{@" + timestamp + ", value={" + value + "}, " + printPayload()
                + "status=" + status
                + ", isOK=" + isOK() + ", isError=" + isError() ;

        if (isOK()) {
            return result + "}";
        }

        return result + ", error=" + error.getClass().getName() + "(" + error.getMessage() + ")}";
    }

    private String printPayload() {
        if (payload == null) {
            return "";
        }

        return "payload(" + payload.getClass().getName() + ")={" + payload.toString() + "}, ";
    }
}
