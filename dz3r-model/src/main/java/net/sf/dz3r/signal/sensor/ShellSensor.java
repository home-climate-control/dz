package net.sf.dz3r.signal.sensor;

import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.SignalSource;
import net.sf.dz3r.signal.filter.TimeoutGuard;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.util.IOUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ShellSensor implements SignalSource<String, Double, Void> {

    private final Logger logger = LogManager.getLogger();
    private final Random rg = new SecureRandom();

    private final Duration defaultPollInterval = Duration.ofSeconds(10);

    private final Map<String, String> address2command;
    private final Map<String, Duration> address2interval = new LinkedHashMap<>();

    /**
     * Create an instance with default poll intervals.
     *
     * @param address2command Mapping from the sensor address to the command that yields its reading.
     */
    public ShellSensor(
            Map<String, String> address2command) {
        this(address2command, Map.of());
    }

    /**
     * Create an instance.
     *
     * @param address2command Mapping from the sensor address to the command that yields its reading.
     * @param address2interval Mapping from the sensor address to its poll interval in seconds (only needed if different from {@link #defaultPollInterval}).
     */
    public ShellSensor(
            Map<String, String> address2command,
            Map<String, Integer> address2interval) {

        this.address2command = address2command;

        Flux.fromIterable(address2interval.entrySet())
                .map(kv -> new AbstractMap.SimpleEntry<>(kv.getKey(), Duration.ofSeconds(kv.getValue())))
                .subscribe(kv -> this.address2interval.put(kv.getKey(), kv.getValue()));
    }

    @Override
    public Flux<Signal<Double, Void>> getFlux(String address) {

        logger.info("getFlux: {}", address);

        var command = address2command.get(address);

        if (command == null) {
            throw new IllegalArgumentException("No command for address '" + address + "'");
        }

        var pollInterval = getPollInterval(address);
        var commandFlux = Flux
                .interval(pollInterval)
                .publishOn(Schedulers.boundedElastic())
                .map(i -> pollInterval)
                .flatMap(timeout -> run(command, timeout));

        return new TimeoutGuard<Double, Void>(pollInterval.multipliedBy(3))
                .compute(commandFlux)
                .share();
    }

    /**
     * Run a single command, kill it after the {@code timeout} if it didn't finish.
     *
     * @param command Command to run.
     * @param timeout How long to wait before forcibly killing the running process.
     *
     * @return A mono with the resulting signal (error signal if the command failed or timed out).
     */
    private Mono<Signal<Double, Void>> run(String command, Duration timeout) {
        ThreadContext.push("run");
        try {
            return Mono.create(sink -> {

                ThreadContext.push("run#" + Integer.toHexString(rg.nextInt()));
                logger.debug("Executing: '/bin/sh -c {}' with timeout of {}", command, timeout);

                Process p = null;

                try {

                    p = Runtime.getRuntime().exec(new String[] {"/bin/sh", "-c", command});
                    var completed = p.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);

                    if (!completed) {

                        // VT: NOTE: This does not *completely* address the issue of stuck commands, it's still lurking
                        // in the background - but at least the rest of the system is not stuck, and the problem is
                        // visible in the logs.

                        // See: https://github.com/home-climate-control/dz/issues/53
                        // See: https://stackoverflow.com/a/808367

                        logger.error("Stuck, ignored: '{}'", command);

                        sink.success(new Signal<>(Instant.now(), null, null, Signal.Status.FAILURE_TOTAL, new TimeoutException("Timeout of " + timeout + " exceeded")));
                        return;
                    }

                    var rc = p.exitValue();

                    if (rc != 0) {
                        // We're screwed
                        logger.error("Command returned error code {}: '{}'", rc, command);
                    }

                    String stdout = readStream(p.getInputStream());
                    String stderr = readStream(p.getErrorStream());

                    var level = rc == 0 ? Level.DEBUG : Level.WARN;
                    logger.log(level, "command: {}", command);
                    logger.log(level, "rc:      {}", rc);
                    logger.log(level, "stdout:  {}", stdout);
                    logger.log(level, "stderr:  {}", stderr);

                    sink.success(rc == 0
                            ? new Signal<>(Instant.now(), Double.parseDouble(stdout))
                            : new Signal<>(Instant.now(), null, null, Signal.Status.FAILURE_TOTAL, new IOException("rc=" + rc + ", see log for details")));

                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    logger.warn("Interrupted", ex);
                    sink.success(new Signal<>(Instant.now(), null, null, Signal.Status.FAILURE_TOTAL, ex));
                } catch (Throwable t) {
                    logger.error("Unexpected exception executing '" + command + "'", t);
                    sink.success(new Signal<>(Instant.now(), null, null, Signal.Status.FAILURE_TOTAL, t));
                } finally {

                    // Unless this is executed, repeated invocations of exec() will
                    // eventually cause the system to run out of file handles
                    if (p != null) {
                        p.destroy();
                    }

                    ThreadContext.pop();
                }
            });
        } catch (Exception ex) {
            return Mono.just(new Signal<>(Instant.now(), null, null, Signal.Status.FAILURE_TOTAL, ex));
        } finally {
            ThreadContext.pop();
        }
    }

    private String readStream(InputStream inputStream) throws IOException {
        var sw = new StringWriter();
        try (var in = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            // Cheating here, this class is from a different package altogether
            IOUtils.copy(in, sw);
        }
        return sw.toString();
    }

    private Duration getPollInterval(String address) {
        var result = address2interval.get(address);

        if (result == null) {
            logger.debug("Using default poll interval of {} for {}", defaultPollInterval, address);
            return defaultPollInterval;
        }

        logger.debug("Using custom poll interval of {} for {}", result, address);
        return result;
    }
}
