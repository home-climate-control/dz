package net.sf.dz3r.controller;

import net.sf.dz3r.signal.Signal;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HysteresisControllerTest {

    @Test
    void testController() {

        Instant timestamp = Instant.now();
        long offset = 0;
        UUID payload = UUID.randomUUID();

        Flux<Signal<Double, UUID>> sequence = Flux.just(
                new Signal<>(timestamp.plus(offset++, ChronoUnit.SECONDS), 20.0, payload),
                new Signal<>(timestamp.plus(offset++, ChronoUnit.SECONDS), 20.5, UUID.randomUUID()),
                new Signal<>(timestamp.plus(offset++, ChronoUnit.SECONDS), 21.0, UUID.randomUUID()),
                new Signal<>(timestamp.plus(offset++, ChronoUnit.SECONDS), 20.5, UUID.randomUUID()),
                new Signal<>(timestamp.plus(offset++, ChronoUnit.SECONDS), 20.0, UUID.randomUUID()),
                new Signal<>(timestamp.plus(offset++, ChronoUnit.SECONDS), 19.5, UUID.randomUUID()),
                new Signal<>(timestamp.plus(offset, ChronoUnit.SECONDS), 19.0, UUID.randomUUID()));

        var pc = new HysteresisController<UUID>("h", 20);

        Flux<Signal<ProcessController.Status<Double>, UUID>> flux = pc
                .compute(sequence);

        StepVerifier
                .create(flux)
                .assertNext(s -> {
                    assertThat(s.getValue().signal).isEqualTo(-1.0);
                    assertThat(s.payload).isInstanceOf(UUID.class);
                    assertThat(s.payload).isEqualTo(payload);
                })
                .assertNext(s -> assertThat(s.getValue().signal).isEqualTo(-1.0))
                .assertNext(s -> assertThat(s.getValue().signal).isEqualTo(1.0))
                .assertNext(s -> assertThat(s.getValue().signal).isEqualTo(1.0))
                .assertNext(s -> assertThat(s.getValue().signal).isEqualTo(1.0))
                .assertNext(s -> assertThat(s.getValue().signal).isEqualTo(1.0))
                .assertNext(s -> assertThat(s.getValue().signal).isEqualTo(-1.0))
                .verifyComplete();
    }
}
