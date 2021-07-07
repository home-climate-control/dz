package net.sf.dz3.view.swing;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.util.Interval;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Averaging tool.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class DoubleAverager {

    private final Logger logger = LogManager.getLogger();

    /**
     * The expiration interval. Values older than the last key by this many
     * milliseconds are expired.
     */
    private long expirationInterval;

    /**
     * The timestamp of the oldest recorded sample.
     */
    private Long oldestTimestamp;

    private int count;
    private double valueAccumulator = 0;

    public DoubleAverager(long expirationInterval) {
        this.expirationInterval = expirationInterval;
    }

    /**
     * Record a value.
     *
     * @param signal Signal to record.
     *
     * @return The average of all data stored in the buffer if this sample is more than {@link #expirationInterval}
     * away from the first sample stored, {@code null} otherwise.
     */
    public Double append(DataSample<? extends Double> signal) {
        return append(signal.timestamp, signal.sample);
    }

    public Double append(long timestamp, Double sample) {


        if (oldestTimestamp == null) {
            oldestTimestamp = timestamp;
        }

        var age = timestamp - oldestTimestamp;

        if ( age < expirationInterval) {

            count++;
            valueAccumulator += sample;

            return null;
        }

        logger.debug("RingBuffer: flushing at {}", () -> Interval.toTimeInterval(age));

        var result = valueAccumulator / count;

        count = 1;
        valueAccumulator = sample;
        oldestTimestamp = timestamp;

        return result;
    }

    public void setExpirationInterval(long expirationInterval) {
        this.expirationInterval = expirationInterval;
    }
}
