package net.sf.dz3r.signal.filter;

import net.sf.dz3r.signal.Signal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterTest {

    private final Logger logger = LogManager.getLogger();
    private final Random rg = new Random();

    @Test
    void testFirst() {

        var rateAllowed = Duration.ofSeconds(1);
        var signal = new Signal<Integer, Void>(Instant.now(), rg.nextInt(), null);
        var source = Flux.just(signal);
        var rl = new RateLimiter<Integer, Void>(rateAllowed, new RateLimiter.Equals<>());
        var limited = rl.compute(source).log();

        StepVerifier
                .create(limited)
                .assertNext(s -> assertThat(s).isEqualTo(signal))
                .verifyComplete();
    }

    @Test
    void testIdenticalTooClose() {

        var rateAllowed = Duration.ofSeconds(1);
        var rateActual = Duration.ofMillis(500);
        var now = Instant.now();
        var then = now.plus(rateActual);

        logger.info("now:  {}", now);
        logger.info("then: {}", then);
        logger.info("diff: {}", Duration.between(then, now));

        var payload = rg.nextInt();

        // Signals are different, but must be treated as equal
        var signal1 = new Signal<Integer, Void>(now, payload, null);
        var signal2 = new Signal<Integer, Void>(then, payload, null);

        var source = Flux
                .just(signal1, signal2)
                .doOnNext(s -> logger.info("source: {}", s));

        var rl = new RateLimiter<Integer, Void>(rateAllowed, new Equals());
        var limited = rl
                .compute(source)
                .doOnNext(s -> logger.info("limited: {}", s));

        StepVerifier
                .create(limited)
                .assertNext(s -> assertThat(s).isEqualTo(signal1))
                .verifyComplete();
    }

    @Test
    void testIdenticalFarEnough() {

        var rateAllowed = Duration.ofSeconds(1);
        var rateActual = rateAllowed.plus(Duration.ofMillis(500));
        var now = Instant.now();
        var then = now.plus(rateActual);

        logger.info("now:  {}", now);
        logger.info("then: {}", then);
        logger.info("diff: {}", Duration.between(then, now));

        var payload = rg.nextInt();

        // Signals are different, but must be treated as equal
        var signal1 = new Signal<Integer, Void>(now, payload, null);
        var signal2 = new Signal<Integer, Void>(then, payload, null);

        var source = Flux
                .just(signal1, signal2)
                .doOnNext(s -> logger.info("source: {}", s));

        var rl = new RateLimiter<Integer, Void>(rateAllowed, new Equals());
        var limited = rl
                .compute(source)
                .doOnNext(s -> logger.info("limited: {}", s));

        StepVerifier
                .create(limited)
                .assertNext(s -> assertThat(s).isEqualTo(signal1))
                .assertNext(s -> assertThat(s).isEqualTo(signal2))
                .verifyComplete();
    }

    @Test
    void testCloseButDifferent() {

        var rateAllowed = Duration.ofSeconds(1);
        var rateActual = Duration.ofMillis(500);
        var now = Instant.now();
        var then = now.plus(rateActual);

        logger.info("now:  {}", now);
        logger.info("then: {}", then);
        logger.info("diff: {}", Duration.between(then, now));

        // Signals are different, but must be treated as equal
        var signal1 = new Signal<Integer, Void>(now, rg.nextInt(), null);
        var signal2 = new Signal<Integer, Void>(then, rg.nextInt(), null);

        var source = Flux
                .just(signal1, signal2)
                .doOnNext(s -> logger.info("source: {}", s));

        var rl = new RateLimiter<Integer, Void>(rateAllowed, new Equals());
        var limited = rl
                .compute(source)
                .doOnNext(s -> logger.info("limited: {}", s));

        StepVerifier
                .create(limited)
                .assertNext(s -> assertThat(s).isEqualTo(signal1))
                .assertNext(s -> assertThat(s).isEqualTo(signal2))
                .verifyComplete();
    }
    @Test
    void testFarEnoughAndDifferent() {

        var rateAllowed = Duration.ofSeconds(1);
        var rateActual = rateAllowed.plus(Duration.ofMillis(500));
        var now = Instant.now();
        var then = now.plus(rateActual);

        logger.info("now:  {}", now);
        logger.info("then: {}", then);
        logger.info("diff: {}", Duration.between(then, now));

        var signal1 = new Signal<Integer, Void>(now, rg.nextInt(), null);
        var signal2 = new Signal<Integer, Void>(then, rg.nextInt(), null);

        var source = Flux
                .just(signal1, signal2)
                .doOnNext(s -> logger.info("source: {}", s));

        var rl = new RateLimiter<Integer, Void>(rateAllowed, new Equals());
        var limited = rl
                .compute(source)
                .doOnNext(s -> logger.info("limited: {}", s));

        StepVerifier
                .create(limited)
                .assertNext(s -> assertThat(s).isEqualTo(signal1))
                .assertNext(s -> assertThat(s).isEqualTo(signal2))
                .verifyComplete();
    }
    public static class Equals implements RateLimiter.Comparator<Integer, Void> {

        @Override
        public boolean equals(Signal<Integer, Void> o1, Signal<Integer, Void> o2) {
            return o1.getValue().equals(o2.getValue());
        }
    }
}
