package net.sf.dz3r.model;

import net.sf.dz3r.signal.Signal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class ZoneTest {

    private final Logger logger = LogManager.getLogger();

    @Test
    void enabled() {

        var signalOK = new Signal<Double, Void>(Instant.now(), 30.0);
        var signalPartialFailure = new Signal<Double, Void>(Instant.now(), 10.0, null, Signal.Status.FAILURE_PARTIAL, new TimeoutException("stale sensor"));
        var signalTotalFailure = new Signal<Double, Void>(Instant.now(), null, null, Signal.Status.FAILURE_TOTAL, new TimeoutException("sensor is gone"));

        var sequence = Flux.just(
                signalOK,
                signalPartialFailure,
                signalTotalFailure
        );

        var ts = new Thermostat("ON", 20, 1, 0, 0, 1);
        var z = new Zone(ts, new ZoneSettings(ts.getSetpoint()));

        var out = z
                .compute(sequence)
                .doOnNext(e -> logger.debug("zone/ON: {}", e));

        StepVerifier
                .create(out)
                .assertNext(s -> assertThat(s.getValue().calling).isTrue())
                .assertNext(s -> assertThat(s.getValue().calling).isFalse())
                .assertNext(s -> assertThat(s.getValue().calling).isFalse())
                .verifyComplete();
    }

    @Test
    void disabled() {

        var signalOK = new Signal<Double, Void>(Instant.now(), 30.0);
        var sequence = Flux.just(signalOK);
        var ts = new Thermostat("ON", 20, 1, 0, 0, 1);
        var settings = new ZoneSettings(ts.getSetpoint());
        var z = new Zone(ts, settings);

        z.set(new ZoneSettings(settings, false));

        var out = z
                .compute(sequence)
                .doOnNext(e -> logger.debug("zone/ON: {}", e));

        // The thermostat is calling, but the zone has shut it off
        StepVerifier
                .create(out)
                .assertNext(s -> assertThat(s.getValue().calling).isFalse())
                .verifyComplete();
    }
}
