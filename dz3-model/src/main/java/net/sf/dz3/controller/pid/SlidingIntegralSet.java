package net.sf.dz3.controller.pid;

import net.sf.dz3.controller.DataSet;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

/**
 * Data set supporting the integration calculation.
 * <p>
 * The {@link DataSet#append} method from {@link DataSet DataSet} class
 * is used, however, make sure you record the right values. If this class is
 * used for the {@link PID_Controller}, it must be fed with controller error,
 * and anti-windup action must be programmed outside of this class.
 *
 * Unlike {@link NaiveIntegralSet} (which has the time complexity of {@code O(n)}), this class
 * provides {@code O(1)} time complexity.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class SlidingIntegralSet implements IntegralSet {

    /**
     * The data set. The key is sampling time, the value is sample value.
     */
    private final LinkedHashMap<Long, Double> samples = new LinkedHashMap<>();

    private final long integrationTime;

    /**
     * Last known timestamp. {@code null} if none recorded yet.
     */
    private Long lastTimestamp;

    private double lastValue;

    private double lastIntegral;

    /**
     * Create the instance.
     *
     * @param integrationTime Integration time, milliseconds. Data elements older than this are expired.
     */
    public SlidingIntegralSet(final long integrationTime) {

        this.integrationTime = integrationTime;
    }

    /**
     * Record the sample.
     *
     * @param millis Absolute time, milliseconds.
     * @param value The sample value.
     */
    @Override
    public synchronized void append(final long millis, final Double value) {

        if (value == null) {
            throw new IllegalArgumentException("null value mustn't propagate here");
        }

        if (lastTimestamp != null && lastTimestamp >= millis) {

            throw new IllegalArgumentException("Data element out of sequence: last key is " + lastTimestamp
                    + ", key being added is " + millis);
        }

        double diff;

        if (lastTimestamp == null) {

            diff = 0;

        } else {

            diff = ((value + lastValue) / 2) * (millis - lastTimestamp);
        }

        lastTimestamp = millis;
        lastValue = value;

        samples.put(millis, diff);

        lastIntegral += diff;

        expire();
    }

    /**
     * Expire all the data elements older than the last by {@link
     * #integrationTime integration time}.
     */
    private void expire() {

        var expireBefore = lastTimestamp - integrationTime;

        Entry<Long, Double> trailer = null;

        for (Iterator<Entry<Long, Double>> i = samples.entrySet().iterator(); i.hasNext(); ) {

            Entry<Long, Double> entry = trailer != null ? trailer : i.next();

            if (entry.getKey() >= expireBefore) {

                // We're done, all other keys will be younger
                return;
            }

            i.remove();

            Entry<Long, Double> next = i.next();

            lastIntegral -= next.getValue();

            trailer = next;
        }
    }

    /**
     * Get the integral starting with the first data element available and
     * ending with the last data element available.
     * <p>
     * Integration time must have been taken care of by {@link DataSet#expire
     * expiration}.
     *
     * @return An integral value (of the {@link #lastIntegral}).
     */
    @Override
    public final synchronized double getIntegral() {
        return lastIntegral;
    }

    public long getSpan() {
        return integrationTime;
    }
}
