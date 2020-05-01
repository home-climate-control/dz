package net.sf.dz3.controller.pid;

import net.sf.dz3.controller.DataSet;

/**
 * Data set supporting the differential calculation.
 *
 * Unlike {@link NaiveDifferentialSet} (which has the time complexity of {@code O(n)}), this class
 * provides {@code O(1)} time complexity.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko</a> 2001-2015
 */
public class SlidingDifferentialSet implements DifferentialSet {

    private final IntegralSet dataSet;

    /**
     * Last known timestamp. {@code null} if none recorded yet.
     */
    private Long lastTimestamp;

    private double lastValue;

    /**
     * Create the instance.
     *
     * @param differentialTime Differential time, milliseconds. Data elements
     * older than this are expired.
     */
    public SlidingDifferentialSet(final long differentialTime) {

        this.dataSet = new SlidingIntegralSet(differentialTime);
    }

    /**
     * Record the sample.
     *
     * @param millis Absolute time, milliseconds.
     * @param value The sample value.
     */
    @Override
    public synchronized void record(final long millis, final Double value) {

        if (value == null) {
            throw new IllegalArgumentException("null value mustn't propagate here");
        }

        if (lastTimestamp != null && lastTimestamp >= millis) {

            throw new IllegalArgumentException("Data element out of sequence: last key is " + lastTimestamp
                    + ", key being added is " + millis);
        }

        if (lastTimestamp != null) {

            double gradient = (value - lastValue) / (millis - lastTimestamp);

            dataSet.record(lastTimestamp + ((millis + lastTimestamp) / 2), gradient);
        }

        lastTimestamp = millis;
        lastValue = value;
    }

    /**
     * Get the differential starting with the first data element available and
     * ending with the last data element available.
     * <p>
     * Differentiation time must have been taken care of by {@link
     * DataSet#expire expiration}.
     *
     * @return A differential value.
     */
    @Override
    public final synchronized double getDifferential() {

        return dataSet.getIntegral();
    }
}
