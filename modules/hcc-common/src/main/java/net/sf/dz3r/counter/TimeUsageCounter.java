package net.sf.dz3r.counter;

import java.time.Duration;

/**
 * Counting durations.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public final class TimeUsageCounter extends AbstractUsageCounter<Duration> {

    public TimeUsageCounter(Duration current, Duration threshold) {
        super(current, threshold);
    }

    @Override
    protected Duration add(Duration current, Duration increment) {
        return current.plus(increment);
    }
}
