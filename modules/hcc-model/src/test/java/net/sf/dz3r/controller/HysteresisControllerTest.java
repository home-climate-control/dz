package net.sf.dz3r.controller;

import com.homeclimatecontrol.hcc.signal.Signal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static com.homeclimatecontrol.hcc.TimeTool.atDayStartUTC;
import static org.assertj.core.api.Assertions.assertThat;

class HysteresisControllerTest {

    private final Logger logger = LogManager.getLogger();

    /**
     * Test the controller behavior with default hysteresis thresholds.
     */
    @ParameterizedTest
    @MethodSource("happyStreamProvider1")
    @MethodSource("errorStreamProvider")
    void testDefault(Flux<TestPair> sequence) {
        var pc = new HysteresisController<UUID>("controller", 20);
        testSequence(sequence, pc);
    }

    private void testSequence(Flux<TestPair> sequence, HysteresisController<UUID> target) {

        var sources = new ArrayList<Signal<Double, UUID>>();
        var expected = new ArrayList<Optional<Double>>();

        sequence.subscribe(p -> {
            sources.add(p.signal);
            expected.add(p.expected);
        });

        var source = Flux.fromIterable(sources);
        var result = target.compute(source);
        var sv = StepVerifier.create(result);

        Flux
                .fromIterable(expected)
                .doOnNext(x -> sv.assertNext(s -> {
                    logger.debug("expected: {}", x);
                    logger.debug("actual: {}", s);
                    assertThat(s.getValue().signal).isEqualTo(x.orElse(null));
                }))
                .subscribe();

        sv.verifyComplete();
    }

    /**
     * Make sure the payload propagates through the controller.
     */
    @Test
    void testPayload() {

        var payload = UUID.randomUUID();
        var sequence = Flux.just(new Signal<>(Instant.now(), 20.0, payload));
        var pc = new HysteresisController<UUID>("h", 20);
        var result = pc.compute(sequence);

        StepVerifier
                .create(result)
                .assertNext(s -> {
                    assertThat(s.getValue().signal).isEqualTo(-1.0);
                    assertThat(s.payload()).isInstanceOf(UUID.class);
                    assertThat(s.payload()).isEqualTo(payload);
                })
                .verifyComplete();
    }

    private static Stream<Flux<TestPair>> happyStreamProvider1() {

        var timestamp = atDayStartUTC();
        long offset = 0;

        return Stream.of(Flux.just(
                new TestPair(new Signal<>(timestamp.plus(offset++, ChronoUnit.SECONDS), 20.0, UUID.randomUUID()), Optional.of(-1.0)),
                new TestPair(new Signal<>(timestamp.plus(offset++, ChronoUnit.SECONDS), 20.5, UUID.randomUUID()), Optional.of(-1.0)),
                new TestPair(new Signal<>(timestamp.plus(offset++, ChronoUnit.SECONDS), 21.0, UUID.randomUUID()), Optional.of(1.0)),
                new TestPair(new Signal<>(timestamp.plus(offset++, ChronoUnit.SECONDS), 20.5, UUID.randomUUID()), Optional.of(1.0)),
                new TestPair(new Signal<>(timestamp.plus(offset++, ChronoUnit.SECONDS), 20.0, UUID.randomUUID()), Optional.of(1.0)),
                new TestPair(new Signal<>(timestamp.plus(offset++, ChronoUnit.SECONDS), 19.5, UUID.randomUUID()), Optional.of(1.0)),
                new TestPair(new Signal<>(timestamp.plus(offset, ChronoUnit.SECONDS), 19.0, UUID.randomUUID()), Optional.of(-1.0))
        ));
    }

    private static Stream<Flux<TestPair>> errorStreamProvider() {

        var timestamp = atDayStartUTC();
        long offset = 0;

        return Stream.of(Flux.just(
                new TestPair(new Signal<>(timestamp.plus(offset++, ChronoUnit.SECONDS), 20.0, UUID.randomUUID()), Optional.of(-1.0)),
                new TestPair(new Signal<>(timestamp.plus(offset++, ChronoUnit.SECONDS), 20.5, UUID.randomUUID()), Optional.of(-1.0)),
                new TestPair(new Signal<>(timestamp.plus(offset++, ChronoUnit.SECONDS), 21.0, UUID.randomUUID()), Optional.of(1.0)),

                // VT: NOTE: This captures the behavior as of rev. acf8b3f044b9d35e483171a60ce1f75694c2d379, which may not be what we actually want in the end.
                // See https://github.com/home-climate-control/dz/issues/339
                new TestPair(new Signal<>(timestamp.plus(offset++, ChronoUnit.SECONDS), null, UUID.randomUUID(), Signal.Status.FAILURE_TOTAL, new IllegalStateException("total")), Optional.empty()),
                new TestPair(new Signal<>(timestamp.plus(offset++, ChronoUnit.SECONDS), 20.5, UUID.randomUUID(), Signal.Status.FAILURE_PARTIAL, new IllegalStateException("total")), Optional.of(1.0)),

                new TestPair(new Signal<>(timestamp.plus(offset++, ChronoUnit.SECONDS), 20.5, UUID.randomUUID()), Optional.of(1.0)),
                new TestPair(new Signal<>(timestamp.plus(offset++, ChronoUnit.SECONDS), 20.0, UUID.randomUUID()), Optional.of(1.0)),
                new TestPair(new Signal<>(timestamp.plus(offset++, ChronoUnit.SECONDS), 19.5, UUID.randomUUID()), Optional.of(1.0)),
                new TestPair(new Signal<>(timestamp.plus(offset, ChronoUnit.SECONDS), 19.0, UUID.randomUUID()), Optional.of(-1.0))
        ));
    }
    private record TestPair(
            Signal<Double, UUID> signal,
            Optional<Double> expected
    ) {

    }
}
