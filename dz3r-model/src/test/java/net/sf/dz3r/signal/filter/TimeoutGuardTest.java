package net.sf.dz3r.signal.filter;

import net.sf.dz3r.signal.Signal;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import reactor.tools.agent.ReactorDebugAgent;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

@Disabled("too flaky on slow and CI/CD boxes")
class TimeoutGuardTest {

    private final Logger logger = LogManager.getLogger();

    /**
     * How much longer the duration needs to be to make slow hosts not to flake out.
     */
    private final double factor = 2;

    private final Duration timeout = Duration.ofMillis((long) (20 * factor));
    private final Duration timeoutDelta = Duration.ofMillis((long) (5 * factor));

    private static final int BACKPRESSURE_COUNT = 50000;

    @BeforeAll
    static void init() {
        ReactorDebugAgent.init();
    }

    @Test
    void nodelay() {

        var guard = new TimeoutGuard<Integer, Void>(timeout, true);

        var source = Flux.range(0, 3).map(v -> new Signal<>(Instant.now(), v, (Void) null));
        var guarded = guard
                .compute(source)

                // Have to enforce this because otherwise everything is gone before the thread can even start
                .take(3);

        // VT: NOTE: I'm not sure StepVerifier.withVirtualTime() will work here

        StepVerifier.create(guarded)

                // There's no delay for all the three input elements

                .assertNext(s -> assertThat(s.getValue()).isZero())
                .assertNext(s -> assertThat(s.getValue()).isEqualTo(1))
                .assertNext(s -> assertThat(s.getValue()).isEqualTo(2))
                .verifyComplete();
    }

    private Flux<Signal<Integer, Void>> createFlux(Duration timeout, TimeoutGuard<Integer, Void> guard) {

        var sequence1 = Flux.just(1, 2, 3);
        var sequence2 = Flux.just(4, 5).delayElements(timeout.plus(timeoutDelta));
        var sequence3 = Flux.just(6, 7, 8).delayElements(timeout.minus(timeoutDelta));
        var sequence4 = Flux.just(9, 10).delaySequence(timeout.plus(timeout).plus(timeout));

        var source = Flux.concat(
                        sequence1,
                        sequence2,
                        sequence3,
                        sequence4)
                .map(v -> new Signal<>(Instant.now(), v, (Void)null));

        var start = Instant.now();
        var last = new AtomicLong(start.toEpochMilli());

        var guarded = guard
                .compute(source)
                .doOnNext(s -> {
                    var level = s.isError() ? Level.WARN : Level.INFO;
                    logger.log(level, "delay: abs {} rel {} = {}", Duration.between(start, s.timestamp).toMillis(), s.timestamp.toEpochMilli() - last.get(), s);
                    last.set(s.timestamp.toEpochMilli());
                });

        return guarded;
    }

    @Test
    void timeoutSingle() {

        var guard = new TimeoutGuard<Integer, Void>(timeout, false);
        var guarded = createFlux(timeout, guard);

//        guarded.blockLast();

        // VT: NOTE: I'm not sure StepVerifier.withVirtualTime() will work here

        StepVerifier.create(guarded)

                // First batch of three goes with no delay to warm up the flux

                .assertNext(s -> assertThat(s.getValue()).isEqualTo(1))
                .assertNext(s -> assertThat(s.getValue()).isEqualTo(2))
                .assertNext(s -> assertThat(s.getValue()).isEqualTo(3))

                // Next two elements are too late and are preceded by timeouts

                .assertNext(s -> {
                    assertThat(s.getValue()).isNull();
                    assertThat(s.isError()).isTrue();
                    assertThat(s.getError()).isInstanceOf(TimeoutException.class);
                })
                .assertNext(s -> assertThat(s.getValue()).isEqualTo(4))

                .assertNext(s -> {
                    assertThat(s.getValue()).isNull();
                    assertThat(s.isError()).isTrue();
                    assertThat(s.getError()).isInstanceOf(TimeoutException.class);
                })
                .assertNext(s -> assertThat(s.getValue()).isEqualTo(5))

                // Next three are just in time

                .assertNext(s -> assertThat(s.getValue()).isEqualTo(6))
                .assertNext(s -> assertThat(s.getValue()).isEqualTo(7))
                .assertNext(s -> assertThat(s.getValue()).isEqualTo(8))

                // But there's a LONG pause before the last two
                // Non-repeating instance will emit just one error signal

                .assertNext(s -> {
                    assertThat(s.getValue()).isNull();
                    assertThat(s.isError()).isTrue();
                    assertThat(s.getError()).isInstanceOf(TimeoutException.class);
                })
                .assertNext(s -> assertThat(s.getValue()).isEqualTo(9))
                .assertNext(s -> assertThat(s.getValue()).isEqualTo(10))

                .verifyComplete();
    }

    @Test
    void timeoutRepeating() {

        var guard = new TimeoutGuard<Integer, Void>(timeout, true);
        var guarded = createFlux(timeout, guard);

        // VT: NOTE: I'm not sure StepVerifier.withVirtualTime() will work here

        StepVerifier.create(guarded)

                // First batch of three goes with no delay to warm up the flux

                .assertNext(s -> assertThat(s.getValue()).isEqualTo(1))
                .assertNext(s -> assertThat(s.getValue()).isEqualTo(2))
                .assertNext(s -> assertThat(s.getValue()).isEqualTo(3))

                // Next two elements are too late and are preceded by timeouts

                .assertNext(s -> {
                    assertThat(s.getValue()).isNull();
                    assertThat(s.isError()).isTrue();
                    assertThat(s.getError()).isInstanceOf(TimeoutException.class);
                })
                .assertNext(s -> assertThat(s.getValue()).isEqualTo(4))

                .assertNext(s -> {
                    assertThat(s.getValue()).isNull();
                    assertThat(s.isError()).isTrue();
                    assertThat(s.getError()).isInstanceOf(TimeoutException.class);
                })
                .assertNext(s -> assertThat(s.getValue()).isEqualTo(5))

                // Next three are just in time

                .assertNext(s -> assertThat(s.getValue()).isEqualTo(6))
                .assertNext(s -> assertThat(s.getValue()).isEqualTo(7))
                .assertNext(s -> assertThat(s.getValue()).isEqualTo(8))

                // But there's a LONG pause before the last two

                .assertNext(s -> {
                    assertThat(s.getValue()).isNull();
                    assertThat(s.isError()).isTrue();
                    assertThat(s.getError()).isInstanceOf(TimeoutException.class);
                })
                .assertNext(s -> {
                    assertThat(s.getValue()).isNull();
                    assertThat(s.isError()).isTrue();
                    assertThat(s.getError()).isInstanceOf(TimeoutException.class);
                })
                .assertNext(s -> {
                    assertThat(s.getValue()).isNull();
                    assertThat(s.isError()).isTrue();
                    assertThat(s.getError()).isInstanceOf(TimeoutException.class);
                })
                .assertNext(s -> assertThat(s.getValue()).isEqualTo(9))
                .assertNext(s -> assertThat(s.getValue()).isEqualTo(10))

                // This is where the signal flux ends, so does everything - we're done
                .verifyComplete();
    }

    @ParameterizedTest
    @MethodSource("rangeFluxProvider")
    void backpressure(Flux<Integer> source) {

        var counter = new AtomicLong();

        try {
            var timeout = Duration.ofMillis(5);
            var guard = new TimeoutGuard<Integer, Void>(timeout, false);
            var signal = source
                    .doOnNext(ignored -> counter.incrementAndGet())
                    .map(i -> new Signal<Integer, Void>(Instant.now(), i));
            var guarded = guard
                    .compute(signal)
                    .take(BACKPRESSURE_COUNT);

            // No matter what the overflow strategy is, it should not break.

            var start = Instant.now();

            assertThatCode(guarded::blockLast).doesNotThrowAnyException();
            assertThat(counter.get()).isEqualTo(BACKPRESSURE_COUNT);

            var elapsed = Duration.between(start, Instant.now()).toMillis();
            logger.info("{}: elapsed {}ms, tps {}", source.getClass().getName(), elapsed, (double) BACKPRESSURE_COUNT * 1000 / elapsed);

        } finally {
            logger.info("{} final count: {}", source.getClass().getName(), counter.get());
        }
    }


    @ParameterizedTest
    @MethodSource("rangeFluxProvider")
    void backpressureUnguarded(Flux<Integer> source) {

        var counter = new AtomicLong();

        try {
            var signal = source
                    .doOnNext(ignored -> counter.incrementAndGet())
                    .map(i -> new Signal<Integer, Void>(Instant.now(), i));

            // We're good no matter what

            assertThatCode(signal::blockLast).doesNotThrowAnyException();

        } finally {
            logger.info("{} count: {}", source.getClass().getName(), counter.get());
        }
    }

    private static Stream<Flux<Integer>> rangeFluxProvider() {
        var source = Flux
                .range(0, BACKPRESSURE_COUNT);

        return Stream.of(
                source.onBackpressureBuffer(),
                source.onBackpressureDrop(),
                source.onBackpressureLatest()
        );
    }

    @Test
    void negativeWaitTime() {

        // As of rev. f26367bf5950a07c7093331149307b7144e20d21:
        // leftToWait.toMillis() <= 0 AND (inTimeout == true AND repeat == false) will cause
        // wait() with negative time

        // Normal operation will yield 0, timeout, 1, timeout, 2
        // Failure will yield something else

        var timeout = Duration.ofMillis(1);
        var guard = new TimeoutGuard<Integer, Void>(timeout, false);
        var signal = Flux.interval(Duration.ofMillis(50))
                .map(i -> new Signal<Integer, Void>(Instant.now(), i.intValue()));
        var guarded = guard
                .compute(signal)
                .log()
                .take(5);

        StepVerifier.create(guarded)

                .assertNext(s -> assertThat(s.getValue()).isZero())

                .assertNext(s -> {
                    assertThat(s.getValue()).isNull();
                    assertThat(s.isError()).isTrue();
                    assertThat(s.getError()).isInstanceOf(TimeoutException.class);
                })

                .assertNext(s -> assertThat(s.getValue()).isEqualTo(1))

                .assertNext(s -> {
                    assertThat(s.getValue()).isNull();
                    assertThat(s.isError()).isTrue();
                    assertThat(s.getError()).isInstanceOf(TimeoutException.class);
                })

                .assertNext(s -> assertThat(s.getValue()).isEqualTo(2))

                .verifyComplete();

    }

    @Test
    void timeoutTooShort() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new TimeoutGuard<Integer, Void>(Duration.ofNanos(1), false))
                .withMessage("Unreasonably short timeout of PT0.000000001S");
    }
}
