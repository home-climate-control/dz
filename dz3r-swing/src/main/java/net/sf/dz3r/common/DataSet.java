package net.sf.dz3r.common;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Entity supporting the data sampling.
 *
 * @author Copyright &copy; <a href="mailto:vt@homaclimatecontrol.com"> Vadim Tkachenko</a> 2001-2021
 */
public class DataSet<T> {

    /**
     * The data set. The key is sampling time, the value is sample value.
     */
    private final Map<Long, T> samples = new LinkedHashMap<>();

    /**
     * The expiration interval. Values older than the last key by this many
     * milliseconds are expired.
     */
    private long expirationInterval;

    /**
     * Strictness. If this is set to true, the {@link #append} will not
     * accept values for the time less than already recorded, and {@link #append
     * } will throw {@code IllegalArgumentException}.
     * <p>
     * This is not necessarily a good thing.
     */
    private final boolean strict;

    /**
     * Last known timestamp. {@code null} if none recorded yet.
     */
    private Long lastTimestamp;

    /**
     * Last known value. {@code null} if none recorded yet.
     */
    private T lastValue;

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

        setExpirationInterval(expirationInterval);
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

    public final void setExpirationInterval(long expirationInterval) {

        if (expirationInterval <= 0) {
            throw new IllegalArgumentException("Expiration interval must be positive, value given is "
                    + expirationInterval);
        }

        this.expirationInterval = expirationInterval;

        // It is less wasteful to check this value here than in append()

        if (lastTimestamp != null) {
            expire();
        }
    }

    /**
     * Record the sample.
     *
     * @param millis Absolute time, milliseconds.
     * @param value The sample value.
     */
    public final synchronized void append(final long millis, final T value) {
        append(millis, value, false);
    }

    /**
     * Record the sample.
     *
     * @param millis Absolute time, milliseconds.
     * @param value The sample value.
     * @param merge if {@code false}, record the value in any case. If {@code true}, record only
     * if it is different from the last one recorded.
     */
    public final synchronized void append(final long millis, final T value, boolean merge) {

        // We don't care if there was a value associated with the given key
        // before, so we return nothing.

        if (strict && lastTimestamp != null && lastTimestamp >= millis) {

                throw new IllegalArgumentException("Data element out of sequence: last key is " + lastTimestamp
                        + ", key being added is " + millis);
        }

        if (lastValue != null && merge && lastValue.equals(value)) {

            // Will replace it with the same value and new timestamp right below. Slower on
            // the way in, faster on the way out.

            samples.remove(lastTimestamp);
        }

        samples.put(millis, value);
        lastValue = value;
        lastTimestamp = millis;

        expire();
    }

    /**
     * Expire all the data elements older than the last by {@link
     * #expirationInterval expiration interval}.
     */
    private void expire() {

        try {

            Long expireBefore = lastTimestamp - expirationInterval;

            for (Iterator<Long> i = samples.keySet().iterator(); i.hasNext();) {

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

    public final Iterator<Map.Entry<Long, T>> entryIterator() {
      return samples.entrySet().iterator();
    }

    /**
     * Get the data set size.
     *
     * @return {@link #samples dataSet} size.
     */
    public final long size() {
        return samples.size();
    }

    public final boolean isEmpty() {
        return samples.isEmpty();
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

        var result = samples.get(time);

        if (result == null) {
            throw new NoSuchElementException("No value for time " + time);
        }

        return result;
    }

    public final void clear() {
        samples.clear();
    }
}
