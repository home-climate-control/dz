package net.sf.dz3r.controller.pid;

import net.sf.dz3r.controller.ProcessController;
import net.sf.dz3r.signal.Signal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;


class SimplePidControllerTest {

    private final Logger logger = LogManager.getLogger();
    private final Random rg = new Random();

    @Test
    void testPSimple() throws InterruptedException {
        testP(new SimplePidController<>("simple", 0d, 1, 0, 0, 0));
    }

    private void testP(ProcessController<Double, Double, Void> pc) {

        ThreadContext.push("testP/" + pc.getClass().getName());

        try {

            // Feel free to push this north of 5,000,000
            int COUNT = 5_000;

            var sourceSequence = new ArrayList<Double>();
            var signalSequence = new ArrayList<Signal<ProcessController.Status<Double>, Void>>();

            var timestamp = new AtomicLong(Instant.now().toEpochMilli());
            Flux<Signal<Double, Void>> sourceFlux = Flux.generate(
                            rg::nextDouble,
                    (state, sink) -> {
                        state = rg.nextDouble();
                        sink.next(state);
                        sourceSequence.add(state);
                        return state;
                    })
                    .map(d -> new Signal<Double, Void>(
                            Instant.ofEpochMilli(timestamp.incrementAndGet()), (Double)d))
                    .take(COUNT);

            var signalFlux = pc.compute(sourceFlux);

            signalFlux
                    .doOnNext(signalSequence::add)
                    .subscribe()
                    .dispose();

            assertThat(sourceSequence).hasSize(COUNT);
            assertThat(signalSequence).hasSize(COUNT);

            int offset = 0;
            for (var v : sourceSequence) {
                assertThat(v).isEqualTo(signalSequence.get(offset++).getValue().signal);
            }

            assertThat(signalSequence.get(0).getValue()).isInstanceOf(PidController.PidStatus.class);

        } finally {
            ThreadContext.pop();
        }
    }

    @Test
    void nullSetpointAtStart() {
        assertThatCode(() -> {
            new SimplePidController<Double>("simple", null, 1, 0, 0, 1);
        }).doesNotThrowAnyException();
    }

    /**
     * Test input and expected output.
     *
     * @param offset Time offset from the start of the stream.
     * @param setpoint Current setpoint.
     * @param pv Current process variable.
     * @param expectedOutput Expected output. Will be interpreted differently depending on what test is run.
     */
    record PidSourceTuple(
            Duration offset,
            Double setpoint,
            double pv,
            double expectedOutput
    ) {

    }

    record PidOutputTuple(
            PidSourceTuple source,
            double actualOutput
    ) {

    }

    @ParameterizedTest
    @MethodSource("getIntegralStream")
    void testIntegral(Flux<PidSourceTuple> source) {
        var c = new SimplePidController<PidSourceTuple>("simple", 20.0, 1, 0.00001, 0, 2);
        var now = Instant.now();
        var signal = source
                .map(t -> tuple2signal(now, t));

        var output = c
                .compute(signal)
                .doOnNext(s -> {
                    logger.debug("output: {}", s);
                    assertThat(((PidController.PidStatus) s.getValue()).i).isEqualTo(s.payload.expectedOutput);
                })
                .blockLast();
    }

    private Signal<Double, PidSourceTuple> tuple2signal(Instant start, PidSourceTuple source) {
        return new Signal<>(start.plus(source.offset), source.pv, source);
    }

    public static Stream<Flux<PidSourceTuple>> getIntegralStream() {

        return Stream.of(
                Flux.just(
                        new PidSourceTuple(Duration.ZERO, 20.0, 20.5, 0),
                        new PidSourceTuple(Duration.ofMinutes(1), 20.0, 20.5, 0.30000000000000004),
                        new PidSourceTuple(Duration.ofMinutes(2), 20.0, 20.5, 0.6000000000000001),
                        new PidSourceTuple(Duration.ofMinutes(3), 20.0, 20.5, 0.9),
                        new PidSourceTuple(Duration.ofMinutes(4), 20.0, 20.5, 1.2000000000000002),
                        new PidSourceTuple(Duration.ofMinutes(5), 20.0, 20.5, 1.5000000000000002),

                        // Integral saturation reached, integral component will no longer change

                        new PidSourceTuple(Duration.ofMinutes(6), 20.0, 20.5, 1.5000000000000002),
                        new PidSourceTuple(Duration.ofMinutes(7), 20.0, 20.5, 1.5000000000000002),
                        new PidSourceTuple(Duration.ofMinutes(8), 20.0, 20.5, 1.5000000000000002),
                        new PidSourceTuple(Duration.ofMinutes(9), 20.0, 20.5, 1.5000000000000002)
                )
        );
    }
}
