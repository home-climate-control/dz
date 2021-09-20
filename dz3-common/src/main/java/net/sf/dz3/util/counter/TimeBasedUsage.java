package net.sf.dz3.util.counter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Simple time based usage counter.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class TimeBasedUsage implements CounterStrategy {

    private final Logger logger = LogManager.getLogger(getClass());

    private long last;
    private boolean running = false;

    public TimeBasedUsage() {

        this(System.currentTimeMillis());
    }

    public TimeBasedUsage(long timestamp) {

        this.last = timestamp;
    }

    /**
     * Interpret {@code value} as "running or not" and use {@code timestamp}
     * to calculate usage.
     *
     * @return milliseconds consumed.
     */
    @Override
    public synchronized long consume(long timestamp, double value) {

        if (timestamp < last) {
            logger.debug("Can't go back in time - timestamp ({} is {}ms less than last known ({}), sample ignored", timestamp, last - timestamp, last);
            return 0;
        }

        long result = running ? timestamp - last : 0;

        last = timestamp;
        running = value > 0;

        return result;
    }
}
