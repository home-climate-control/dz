package net.sf.dz3r.view.influxdb.v3;

import net.sf.dz3r.signal.Signal;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.tools.agent.ReactorDebugAgent;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * InfluxDB logger test.
 *
 * Verify this by running the following snippet on the target InfluxDB instance:
 * {@code use dz-test}
 * {@code select * from sensor where instance = 'dz3.test.v3'}
 */
@Disabled("Enable if you have InfluxDB running on localhost (or elsewhere, see the source)")
class InfluxDbLoggerTest {

    private static final Random rg = new SecureRandom();

    private static InfluxDbLogger dbLogger;

    private synchronized InfluxDbLogger getLogger() {

        if (dbLogger == null) {
            dbLogger = new InfluxDbLogger("dz-test", "dz3.test.v3", "http://127.0.0.1:8086", null, null);
        }

        return dbLogger;
    }

    @BeforeAll
    static void init() {
        ReactorDebugAgent.init();
    }

    @Test
    void feed() {
        assertThatCode(() -> {

            var sequence = Flux.just(
                    new Signal<Number, Map<String, String>>(
                            Instant.now(),
                            rg.nextDouble(),
                            Map.of(
                                    "source", "test",
                                    "name", "feed",
                                    "kind", "regular")),
                    new Signal<Number, Map<String, String>>(
                            Instant.now(),
                            rg.nextDouble(),
                            Map.of(
                                    "source", "test",
                                    "name", "feed",
                                    "kind", "partial failure"),
                            Signal.Status.FAILURE_PARTIAL,
                            new TimeoutException()),
                    new Signal<Number, Map<String, String>>(
                            Instant.now(),
                            null,
                            Map.of(
                                    "source", "test",
                                    "name", "feed",
                                    "kind", "total failure"),
                            Signal.Status.FAILURE_TOTAL,
                            new IllegalStateException()));

            sequence.subscribe(getLogger());

        }).doesNotThrowAnyException();
    }
}
