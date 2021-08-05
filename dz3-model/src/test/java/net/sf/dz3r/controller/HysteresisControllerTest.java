package net.sf.dz3r.controller;

import net.sf.dz3r.signal.Signal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class HysteresisControllerTest {

    private final Logger logger = LogManager.getLogger();
    private final Random rg = new Random();

    @Test
    void testController() throws InterruptedException {

        Instant timestamp = Instant.now();
        long offset = 0;

        var sequence = Flux.just(
                new Signal<>(timestamp.plus(offset++, ChronoUnit.SECONDS), 20.0),
                new Signal<>(timestamp.plus(offset++, ChronoUnit.SECONDS), 20.5),
                new Signal<>(timestamp.plus(offset++, ChronoUnit.SECONDS), 21.0),
                new Signal<>(timestamp.plus(offset++, ChronoUnit.SECONDS), 20.5),
                new Signal<>(timestamp.plus(offset++, ChronoUnit.SECONDS), 20.0),
                new Signal<>(timestamp.plus(offset++, ChronoUnit.SECONDS), 19.5),
                new Signal<>(timestamp.plus(offset, ChronoUnit.SECONDS), 19.0));

        var pc = new HysteresisController("h", 20);

        Flux<Signal<ProcessController.Status<Double>>> flux = pc
                .compute(sequence);

        StepVerifier
                .create(flux)
                .assertNext(s -> assertThat(s.getValue().signal).isEqualTo(-1.0))
                .assertNext(s -> assertThat(s.getValue().signal).isEqualTo(-1.0))
                .assertNext(s -> assertThat(s.getValue().signal).isEqualTo(1.0))
                .assertNext(s -> assertThat(s.getValue().signal).isEqualTo(1.0))
                .assertNext(s -> assertThat(s.getValue().signal).isEqualTo(1.0))
                .assertNext(s -> assertThat(s.getValue().signal).isEqualTo(1.0))
                .assertNext(s -> assertThat(s.getValue().signal).isEqualTo(-1.0))
                .verifyComplete();
    }
}
