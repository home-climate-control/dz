package net.sf.dz3.controller;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Entity supporting the data sampling.
 *
 * VT: FIXME: Implement variable expiration time.
 *
 * This is the old implementation, written in 2000 with little regard to performance.
 *
 * @see DataSet
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2012
 */
public class LegacyDataSet<T> {

    /**
     * The data set. The key is sampling time, the value is sample value.
     */
    private final SortedMap<Long, T> samples = new TreeMap<>();

    /**
     * The expiration interval. Values older than the last key by this many
     * milliseconds are expired.
     */
    private long expirationInterval;

    /**
     * Strictness. If this is set to true, the {@link #append} will not
     * accept values for the time less than already recorded, and {@link #append}
     * will throw {@code IllegalArgumentException}.
     * <p>
     * This is not necessarily a good thing.
     */
    private boolean strict = false;

    /**
     * Create the instance.
     *
     * @param expirationInterval How many milliseconds to keep the data.
     *
     * @exception IllegalArgumentException if the expiration interval is
     * non-positive (<= 0). Be careful with the short intervals, it's going to
     * be your fault, not mine.
     */
    public LegacyDataSet(final long expirationInterval) {

        if (expirationInterval <= 0) {

            throw new IllegalArgumentException("Expiration interval must be positive, value given is "
                    + expirationInterval);
        }

        this.expirationInterval = expirationInterval;
    }

    /**
     * Set strictness.
     *
     * @param strict If set to true, out-of-order updates will not be accepted.
     */
    public final synchronized void setStrict(final boolean strict) {
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
    public final synchronized void append(final long millis, final T value) {

        // We don't care if there was a value associated with the given key
        // before, so we return nothing.

        if (strict) {

            try {

                Long lastKey = samples.lastKey();

                if (lastKey >= millis) {

                    throw new IllegalArgumentException("Data element out of sequence: last key is " + lastKey
                            + ", key being added is " + millis);
                }

            } catch (NoSuchElementException nseex) {

                // We're fine. This means that there was no previous entry,
                // therefore, the current one will not be out of order.
            }
        }

        samples.put(millis, value);

        expire();
    }

    /**
     * Expire all the data elements older than the last by {@link
     * #expirationInterval expiration interval}.
     */
    private void expire() {

        try {

            var lastKey = samples.lastKey();
            var expireBefore = lastKey - expirationInterval;

            SortedMap<Long, T> expireMap = samples.headMap(expireBefore);

            for (Iterator<Long> i = expireMap.keySet().iterator(); i.hasNext();) {

                i.next();
                i.remove();
            }

        } catch (NoSuchElementException nseex) {
            // We're fine, the map is empty
        }
    }

    /**
     * @return Iterator on the time values for the data entries.
     */
    public final Iterator<Long> iterator() {

        return samples.keySet().iterator();
    }

    protected final Iterator<Map.Entry<Long, T>> entryIterator() {

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
}
