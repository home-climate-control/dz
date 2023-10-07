package net.sf.dz3r.counter;

import net.sf.dz3r.common.HCCObjects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/**
 * Read time from file, count, store back.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class FileTimeUsageCounter implements ResourceUsageCounter<Duration>, AutoCloseable {

    private final Logger logger = LogManager.getLogger();

    private static final String CF_THRESHOLD = "threshold";
    private static final String CF_CURRENT = "current";

    private final String marker;
    private final Duration defaultThreshold;

    private final File storage;

    private final Set<ResourceUsageReporter<Duration>> reporters;

    private final TimeUsageCounter counter;

    private final Sinks.Many<State<Duration>> saveSink = Sinks.many().unicast().onBackpressureBuffer();

    private final Disposable saveSubscription;

    public FileTimeUsageCounter(String marker, Duration defaultThreshold, File storage, Set<ResourceUsageReporter<Duration>> reporters) throws IOException {

        this.marker = HCCObjects.requireNonNull(marker, "marker can't be null");
        this.defaultThreshold = HCCObjects.requireNonNull(defaultThreshold, "defaultThreshold can't be null");

        this.storage = checkSanity(storage);
        this.reporters = HCCObjects.requireNonNull(reporters, "reporters can't be null");

        this.counter = load(storage);

        saveSubscription = saveSink
                .asFlux()
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(this::save)
                .doOnNext(this::report)
                .subscribe();
    }

    private File checkSanity(File target) throws IOException {

        HCCObjects.requireNonNull(target, "storage can't be null");

        File canonical = new File(target.getCanonicalPath());
        if (canonical.getParentFile().mkdirs()) {
            logger.info("created {}", canonical);
        }

        if (target.isDirectory()) {
            throw new IllegalArgumentException(target + ": is a directory");
        }

        if (target.exists()) {

            if (!target.canWrite()) {
                throw new IllegalArgumentException(target + ": can't write");
            }

            if (!target.isFile()) {
                throw new IllegalArgumentException(target + ": not a regular file");
            }
        }

        return target;
    }

    private TimeUsageCounter load(File source) throws IOException {

        ThreadContext.push("load#" + marker);

        try {

            logger.info("reading from {}", source);

            if (!storage.exists()) {

                logger.warn("{} doesn't exist, will initialize with 0/{}", source, defaultThreshold);
                return new TimeUsageCounter(Duration.ZERO, defaultThreshold);
            }

            var p = new Properties();

            try (var in = new FileInputStream(storage)) {

                p.load(in);

                var thresholdString = p.getProperty(CF_THRESHOLD);
                var currentString = p.getProperty(CF_CURRENT);

                if (thresholdString == null) {
                    // Not fatal, just unusual
                    logger.warn("No '{}=NN' found in {}, assuming no threshold", CF_THRESHOLD, storage);
                }

                if (currentString == null) {
                    // Fatal
                    throw new IllegalArgumentException("No '" + CF_CURRENT + "=NN' found in " + storage);
                }

                var threshold = Optional.ofNullable(thresholdString).map(Duration::parse).orElse(Duration.ZERO);
                var current = Duration.parse(currentString);

                return new TimeUsageCounter(current, threshold);
            }

        } finally {
            ThreadContext.pop();
        }
    }

    @Override
    public Flux<State<Duration>> consume(Flux<Duration> increments) {
        return counter
                .consume(increments)
                .doOnNext(this::sync);
    }

    private void sync(State<Duration> state) {
        saveSink.tryEmitNext(state);
    }

    private void save(State<Duration> state) {
        ThreadContext.push("save#" + marker);

        try {

            // VT: NOTE: See https://github.com/home-climate-control/dz/issues/102 for background.
            // Previous experience shows that no matter what you do, you fail this way or that, so we'll be simple here.
            // Let reporters take care of bulletproofing this, we're just the driver.

            File canonical = new File(storage.getCanonicalPath());
            File backup = new File(canonical.getParent(), canonical.getName() + "-");

            if (canonical.exists() && !canonical.renameTo(backup)) {
                logger.error("failed to rename {} to {}", canonical, backup);
            }

            try (PrintWriter pw = new PrintWriter(new FileWriter(storage))) {

                pw.println("# Resource Usage Counter: " + marker);

                var usage = state.threshold().toMillis() == 0
                        ? 0
                        : (double) state.current().toMillis() / (double) state.threshold().toMillis();

                pw.println("#");
                pw.println(String.format("# Relative usage %2.0f%%", usage * 100) + (usage > 1 ? " (OVERDUE)" : ""));
                pw.println("#");

                pw.println("# " + CF_THRESHOLD + "=" + getHumanReadableTime(state.threshold()));
                pw.println("# " + CF_CURRENT + "=" + getHumanReadableTime(state.current()));

                pw.println("#");

                pw.println(CF_THRESHOLD + "=" + state.threshold());
                pw.println(CF_CURRENT + "=" + state.current());
            }

            logger.debug("saved to {}", storage);

        } catch (IOException ex) {
            // VT: NOTE: Nothing we can do about it now, let's pester the user with error logs,
            // they're bound to notice eventually
            logger.error("can't save to {}", storage, ex);

        } finally {
            ThreadContext.pop();
        }
    }

    static String getHumanReadableTime(Duration d) {

        var hours = d.toHours();

        if (hours > 0) {
            return hours + " hours";
        }

        var minutes = d.toMinutes();

        if (minutes > 0) {
            return minutes + " minutes";
        }

        var seconds = d.toSeconds();

        if (seconds > 0) {
            return seconds + " seconds";
        }

        // The hell with it
        return d.toMillis() + " ms";
    }

    private void report(State<Duration> state) {

        for (var r : reporters) {
            try {
                // Can't afford to fail here, need to catch everything
                r.report(state);
            } catch (Exception ex) {
                logger.error("can't report on state={}", state, ex);
            }
        }
    }

    @Override
    public void close() throws Exception {
        saveSink.tryEmitComplete();
        saveSubscription.dispose();
    }
}
