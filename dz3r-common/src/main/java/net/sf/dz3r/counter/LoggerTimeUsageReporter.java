package net.sf.dz3r.counter;

import net.sf.dz3r.common.HCCObjects;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static net.sf.dz3r.counter.FileTimeUsageCounter.getHumanReadableTime;

/**
 * Simple reporter to complement {@link FileTimeUsageCounter}.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class LoggerTimeUsageReporter implements ResourceUsageReporter<Duration> {

    private final Logger logger = LogManager.getLogger();
    private final Map<Level, Duration> frequencyMap = Map.of(
            Level.DEBUG, Duration.of(1, ChronoUnit.HOURS),
            Level.INFO, Duration.of(1, ChronoUnit.HOURS),
            Level.WARN, Duration.of(30, ChronoUnit.MINUTES),
            Level.ERROR, Duration.of(10, ChronoUnit.MINUTES)
    );

    private Instant lastAlertIssued;
    private final String marker;

    /**
     * Create an instance.
     *
     * @param marker Marker to use when logging.
     */
    public LoggerTimeUsageReporter(String marker) {
        this.marker = HCCObjects.requireNonNull(marker, "marker can't be null");
    }

    @Override
    public void report(ResourceUsageCounter.State<Duration> state) {

        ThreadContext.push("alert#" + marker);

        try {

            if (state.threshold().equals(Duration.ZERO)) {
                logger.trace("threshold not set");
                logger.debug("current={} {}", state.current(), getHumanReadableTime(state.current()));
                return;
            }

            var usage = state.threshold().toMillis() == 0
                    ? 0
                    : (double) state.current().toMillis() / (double) state.threshold().toMillis();
            var percent = usage * 100;

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

            logger.log(level, "{}: current usage {}%{}", marker, (int) percent, (percent > 100 ? " (OVERDUE)" : ""));
            lastAlertIssued = now;

        } finally {
            ThreadContext.pop();
        }
    }
}
