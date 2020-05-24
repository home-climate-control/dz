package net.sf.dz3.controller;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Entity supporting the data sampling.
 *
 * VT: FIXME: Implement variable expiration time.
 *
 * @author Copyright &copy; <a href="mailto:vt@homaclimatecontrol.com"> Vadim Tkachenko</a> 2001-2020
 */
public class DataSet<T> {

    /**
     * The data set. The key is sampling time, the value is sample value.
     */
    private LinkedHashMap<Long, T> dataSet = new LinkedHashMap<>();

    /**
     * The expiration interval. Values older than the last key by this many
     * milliseconds are expired.
     */
    private long expirationInterval;

    /**
     * Strictness. If this is set to true, the {@link #record record()} will not
     * accept values for the time less than already recorded, and {@link #record
     * record()} will throw {@codeIllegalArgumentException}.
     * <p>
     * This is not necessarily a good thing.
     */
    private final boolean strict;

    /**
     * Last known timestamp. {@code null} if none recorded yet.
     */
    private Long lastTimestamp;

    /**
     * Create the instance allowing out-of-order updates.
     *
     * @param expirationInterval How many milliseconds to keep the data.
     * @exception IllegalArgumentException if the expiration interval is
     * non-positive (<= 0). Be careful with the short intervals, it's going to
     * be your fault, not mine.
     */
    public DataSet(final long expirationInterval) {
        this(expirationInterval, false);
    }

    /**
     * Create the instance.
     *
     * @param expirationInterval How many milliseconds to keep the data.
     *
     * @param strict If set to true, out-of-order updates will not be accepted.
     *
     * @exception IllegalArgumentException if the expiration interval is
     * non-positive (<= 0). Be careful with the short intervals, it's going to
     * be your fault, not mine.
     */
    public DataSet(final long expirationInterval, boolean strict) {

        if (expirationInterval <= 0) {

            throw new IllegalArgumentException("Expiration interval must be positive, value given is "
                    + expirationInterval);
        }

        this.expirationInterval = expirationInterval;
        this.strict = strict;
    }

    /**
     * Get the expiration interval.
     *
     * @return Expiration interval, milliseconds.
     */
    public final long getExpirationInterval() {

        return expirationInterval;
    }

    /**
     * Record the sample.
     *
     * @param millis Absolute time, milliseconds.
     * @param value The sample value.
     */
    public final synchronized void record(final long millis, final T value) {

        // We don't care if there was a value associated with the given key
        // before, so we return nothing.

        if (strict && lastTimestamp != null && lastTimestamp >= millis) {

                throw new IllegalArgumentException("Data element out of sequence: last key is " + lastTimestamp
                        + ", key being added is " + millis);
        }

        lastTimestamp = millis;

        dataSet.put(Long.valueOf(millis), value);

        expire();
    }

    /**
     * Expire all the data elements older than the last by {@link
     * #expirationInterval expiration interval}.
     */
    private final void expire() {

        try {

            Long expireBefore = Long.valueOf(lastTimestamp.longValue() - expirationInterval);

            for (Iterator<Long> i = dataSet.keySet().iterator(); i.hasNext();) {

                Long found = i.next();

                if (found < expireBefore) {

                    i.remove();

                } else {

                    // We're done, all other keys will be younger
                    return;
                }

            }

        } catch (NoSuchElementException nseex) {

            // We're fine, the map is empty
        }
    }

    /**
     * @return Iterator on the time values for the data entries.
     */
    public final Iterator<Long> iterator() {

        return dataSet.keySet().iterator();
    }

    public final Iterator<Map.Entry<Long, T>> entryIterator() {

      return dataSet.entrySet().iterator();
    }

    /**
     * Get the data set size.
     *
     * @return {@link #dataSet dataSet} size.
     */
    public final long size() {

        return dataSet.size();
    }

    /**
     * Get the value recorded at the given time.
     *
     * @param time Time to look up the data for. Must be exact, otherwise,
     * exception will be thrown.
     * @return Value recorded at the given time.
     *
     * @exception NoSuchElementException if the value for the given time is not
     * in the set.
     */
    public final T get(final long time) {

        T result = dataSet.get(Long.valueOf(time));

        if (result == null) {

            throw new NoSuchElementException("No value for time " + time);
        }

        return result;
    }
}