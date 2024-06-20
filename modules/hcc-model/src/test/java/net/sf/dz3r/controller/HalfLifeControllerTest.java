package net.sf.dz3r.controller;

import net.sf.dz3r.signal.Signal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class HalfLifeControllerTest {

    private final Logger logger = LogManager.getLogger();

    @Test
    void goodSetpoint() {
        var c = new HalfLifeController<HalfLifeTuple>("goodSetpoint", Duration.ofSeconds(30));
        assertThatCode(() -> {
            c.setSetpoint(0.0);
        }).doesNotThrowAnyException();
    }

    @Test
    void badSetpoint() {
        var c = new HalfLifeController<HalfLifeTuple>("badSetpoint", Duration.ofSeconds(30));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> {
                    c.setSetpoint(20.0);
                });
    }

    @ParameterizedTest
    @MethodSource("getSignalStream")
    void testDecay(Flux<HalfLifeTuple> source) {
        var c = new HalfLifeController<HalfLifeTuple>("decay", Duration.ofSeconds(30));
        var now = Instant.now();
        var signal = source
                .map(t -> tuple2signal(now, t));

        var output = c
                .compute(signal)
                .doOnNext(s -> {
                    logger.debug("output: {}", s);
                    assertThat((s.getValue()).signal).isCloseTo(s.payload.expectedOutput, Offset.offset(0.000001));
                })
                .blockLast();
    }

    record HalfLifeTuple(
            Duration offset,
            double pv,
            double expectedOutput
    ) {

    }

    private Signal<Double, HalfLifeTuple> tuple2signal(Instant start, HalfLifeTuple source) {
        return new Signal<>(start.plus(source.offset), source.pv, source);
    }

    public static Stream<Flux<HalfLifeTuple>> getSignalStream() {

        return Stream.of(

                // Simple stream - one change within half-life duration
                Flux.just(
                        new HalfLifeTuple(Duration.ZERO, 20.0,0),

                        // Generate the diff of 1.0
                        new HalfLifeTuple(Duration.ofSeconds(1), 21.0, 1),

                        new HalfLifeTuple(Duration.ofSeconds(16), 21.0, 0.707106781186548),
                        new HalfLifeTuple(Duration.ofSeconds(31), 21.0, 0.5),
                        new HalfLifeTuple(Duration.ofSeconds(61), 21.0, 0.25),
                        new HalfLifeTuple(Duration.ofSeconds(91), 21.0, 0.125),
                        new HalfLifeTuple(Duration.ofSeconds(121), 21.0, 0.0625)
                ),

                // Complex stream - multiple changes within half-life duration
                Flux.just(
                        new HalfLifeTuple(Duration.ZERO, 20.0,0),

                        new HalfLifeTuple(Duration.ofSeconds(1), 22.0, 2),

                        // Input changed, we have a new baseline here...
                        new HalfLifeTuple(Duration.ofSeconds(31), 24.0, 4),

                        // ...which decayed exactly as expected.
                        new HalfLifeTuple(Duration.ofSeconds(61), 24.0, 2),
                        new HalfLifeTuple(Duration.ofSeconds(91), 24.0, 1)
                )
        );
    }
}
