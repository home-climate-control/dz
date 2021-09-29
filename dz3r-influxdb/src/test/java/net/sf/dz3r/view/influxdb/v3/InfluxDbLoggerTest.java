package net.sf.dz3r.view.influxdb.v3;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.influxdb.dto.Point;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import reactor.core.publisher.Flux;
import reactor.tools.agent.ReactorDebugAgent;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.fail;

/**
 * InfluxDB logger test.
 *
 * Verify this by running the following snippet on the target InfluxDB instance:
 * {@code use dz-test}
 * {@code select * from sensor where instance = 'dz3.test.v3'}
 */
@Disabled("Enable if you have InfluxDB running on localhost (or elsewhere, see the source)")
class InfluxDbLoggerTest {

    private final Logger logger = LogManager.getLogger();
    private static final Random rg = new SecureRandom();

    private static InfluxDbLogger dbLogger;

    private synchronized InfluxDbLogger getLogger() {

        if (dbLogger == null) {
            dbLogger = new InfluxDbLogger(
                    "dz-test",
                    "dz3.test.v3",
                    "http://127.0.0.1:8086",
                    null, null,
                    Map.of());
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
                    Point.measurement("InfluxDbLogger")
                            .time(Instant.now().toEpochMilli(), TimeUnit.MILLISECONDS)
                            .tag("source", "test")
                            .tag("kind", "regular")
                            .addField("double", rg.nextDouble())
                            .build(),
                    Point.measurement("InfluxDbLogger")
                            .time(Instant.now().toEpochMilli(), TimeUnit.MILLISECONDS)
                            .tag("source", "test")
                            .tag("kind", "failure")
                            .addField("error", "oops")
                            .build()
            );

            var ok = new AtomicBoolean(true);
            sequence
                    .log()
                    .doOnError(t -> {
                        logger.error("Oops", t);
                        ok.set(false);
                    })
                    .subscribe((Subscriber) getLogger());

            if (!ok.get()) {
                fail("Abnormal termination, check the logs");
            }

        }).doesNotThrowAnyException();
    }
}
