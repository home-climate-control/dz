package net.sf.dz3r.signal;

import net.sf.dz3r.device.Addressable;

import java.time.Instant;
import java.util.Optional;

/**
 * Base interface for all the signals in the system.
 *
 * @param <A> Address type.
 * @param <S> Signal source reference type.
 * @param <V> Signal value type.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2021
 */
public class Signal<A extends Comparable<A>, S extends Addressable<A>, V> {

    public enum Status {
        OK,
        FAILURE_PARTIAL,
        FAILURE_TOTAL
    }

    public final Instant timestamp;
    public final S source;
    private final V value;
    public final Status status;
    public final Throwable error;

    /**
     * Construct a non-error signal.
     *
     * @param timestamp Signal timestamp.
     * @param source Signal source.
     * @param value Signal value.
     */
    public Signal(Instant timestamp, S source, V value) {
        this(timestamp, source, value, Status.OK, null);
    }

    public Signal(Instant timestamp, S source, V value, Status status, Throwable error) {

        if (timestamp == null) {
            throw new IllegalArgumentException("timestamp can't be null");
        }

        if (source == null) {
            throw new IllegalArgumentException("source can't be null");
        }

        if (value == null && status == Status.OK) {
            throw new IllegalArgumentException("null value doesn't make sense for status OK");
        }

        this.timestamp = timestamp;
        this.source = source;
        this.value = value;

        this.status = status;
        this.error = error;
    }

    public S getSource() {
        return source;
    }

    public Optional<V> getValue() {
        return value == null ? Optional.empty() : Optional.of(value);
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

    public Status getStatus() {
        return status;
    }

    public Throwable getError() {
        return error;
    }
}
