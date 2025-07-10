package net.sf.dz3r.controller;

import com.homeclimatecontrol.hcc.TimeTool;
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
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class HysteresisControllerTest {

    private final Logger logger = LogManager.getLogger();

    @ParameterizedTest
    @MethodSource("happyStreamProvider1")
    void testHappyDefault(Flux<TestPair> sequence) {
        var pc = new HysteresisController<UUID>("controller", 20);
        testSequence(sequence, pc);
    }

    private void testSequence(Flux<TestPair> sequence, HysteresisController<UUID> target) {

        var sources = new ArrayList<Signal<Double, UUID>>();
        var expected = new ArrayList<Double>();

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
                    assertThat(s.getValue().signal).isEqualTo(x);
                }))
                .subscribe();

        sv.verifyComplete();
    }

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

        var timestamp = TimeTool.atMidnightUTC();
        long offset = 0;

        return Stream.of(Flux.just(
                new TestPair(new Signal<>(timestamp.plus(offset++, ChronoUnit.SECONDS), 20.0, UUID.randomUUID()), -1.0),
                new TestPair(new Signal<>(timestamp.plus(offset++, ChronoUnit.SECONDS), 20.5, UUID.randomUUID()), -1.0),
                new TestPair(new Signal<>(timestamp.plus(offset++, ChronoUnit.SECONDS), 21.0, UUID.randomUUID()), 1.0),
                new TestPair(new Signal<>(timestamp.plus(offset++, ChronoUnit.SECONDS), 20.5, UUID.randomUUID()), 1.0),
                new TestPair(new Signal<>(timestamp.plus(offset++, ChronoUnit.SECONDS), 20.0, UUID.randomUUID()), 1.0),
                new TestPair(new Signal<>(timestamp.plus(offset++, ChronoUnit.SECONDS), 19.5, UUID.randomUUID()), 1.0),
                new TestPair(new Signal<>(timestamp.plus(offset, ChronoUnit.SECONDS), 19.0, UUID.randomUUID()), -1.0)
        ));
    }

    private record TestPair(
            Signal<Double, UUID> signal,
            Double expected
    ) {

    }
}
