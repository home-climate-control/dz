package net.sf.dz3.util.counter;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSource;
import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * Usage counter proof of concept with no persistent state.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class TransientUsageCounter extends AbstractUsageCounter {

    private final Map<Level, Duration> frequencyMap = Map.of(
            Level.DEBUG, Duration.of(1, ChronoUnit.HOURS),
            Level.INFO, Duration.of(1, ChronoUnit.HOURS),
            Level.WARN, Duration.of(10, ChronoUnit.MINUTES),
            Level.ERROR, Duration.of(1, ChronoUnit.MINUTES)
    );

    private Instant lastAlertIssued;

    /**
     * Create an instance with the default (time based usage) counter strategy and no storage keys.
     *
     * @param name Human readable name for the user interface.
     * @param target What to count.
     * @param isTime Whether the countable resource is time.
     *
     * @throws IOException if things go sour.
     */
    public TransientUsageCounter(String name, DataSource<Double> target, boolean isTime) throws IOException {
        super(name, target, isTime, null);
    }

    /**
     * Create an instance with no storage keys.
     *
     * @param name Human readable name for the user interface.
     * @param counter Counter to use.
     * @param target What to count.
     * @param isTime Whether the countable resource is time.
     *
     * @throws IOException if things go sour.
     */
    public TransientUsageCounter(String name, CounterStrategy counter, DataSource<Double> target, boolean isTime) throws IOException {
        super(name, counter, target, isTime, null);
    }

    /**
     * Create an instance.
     *
     * @param name Human readable name for the user interface.
     * @param counter Counter to use.
     * @param target What to count.
     * @param isTime Whether the countable resource is time.
     * @param storageKeys How to store the counter data.
     *
     * @throws IOException if things go sour.
     */
    public TransientUsageCounter(String name, CounterStrategy counter, DataSource<Double> target, boolean isTime, Object[] storageKeys) throws IOException {
        super(name, counter, target, isTime, storageKeys);
    }

    @Override
    protected void alert(long threshold, long current) {

        ThreadContext.push("alert@" + Integer.toHexString(hashCode()));

        try {

            if (threshold == 0) {
                logger.debug("Threshold not set");
                return;
            }

            long percent = (long)(getUsageRelative() * 100);

            Level level;

            if (percent < 50) {
                level = Level.DEBUG;
            } else if (percent < 80) {
                level = Level.INFO;
            } else if (percent < 100) {
                level = Level.WARN;
            } else {
                level = Level.ERROR;
            }

            var now = Instant.now();
            var duration = frequencyMap.get(level);

            if (lastAlertIssued != null && Duration.between(lastAlertIssued, now).compareTo(duration) < 0) {
                // Too fresh, not logging
                return;
            }

            String message = getName() + ": current usage " + percent + "%" + (percent > 100 ? " (OVERDUE)" : "");

            logger.log(level, message);
            lastAlertIssued = now;

        } finally {
            ThreadContext.pop();
        }
    }

    @Override
    protected CounterState load() throws IOException {

        // One hour limit
        return new CounterState(1000L * 60L * 60L, 0);
    }

    @Override
    protected void save() throws IOException {

        ThreadContext.push("save@" + Integer.toHexString(hashCode()));

        try {
            // Do absolutely nothing except logging the current usage
            logger.info("Current usage: {}/{} ({})", getUsageAbsolute(), getThreshold(), getUsageRelative());

        } finally {
            ThreadContext.pop();
        }
    }

    @Override
    public JmxDescriptor getJmxDescriptor() {

        return new JmxDescriptor(
                "dz",
                "Resource Usage Counter",
                getName(),
                "Keeps track of resource usage and logs it");
    }

    @Override
    protected void doReset() throws IOException {
        // Do absolutely nothing
    }
}
