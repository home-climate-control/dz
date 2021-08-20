package net.sf.dz3r.model;

import net.sf.dz3r.signal.Signal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class ZoneTest {

    private final Logger logger = LogManager.getLogger();

    @Test
    void enabled() {

        var setpoint = 20.0;
        var signalOK = new Signal<Double, String>(Instant.now(), 30.0);
        var signalPartialFailure = new Signal<Double, String>(Instant.now(), 10.0, null, Signal.Status.FAILURE_PARTIAL, new TimeoutException("stale sensor"));
        var signalTotalFailure = new Signal<Double, String>(Instant.now(), null, null, Signal.Status.FAILURE_TOTAL, new TimeoutException("sensor is gone"));

        var sequence = Flux.just(
                signalOK,
                signalPartialFailure,
                signalTotalFailure
        );

        var name = UUID.randomUUID().toString();
        var ts = new Thermostat(name, setpoint, 1, 0, 0, 1);
        var z = new Zone(ts, new ZoneSettings(ts.getSetpoint()));

        var out = z
                .compute(sequence)
                .doOnNext(e -> logger.debug("zone/ON: {}", e));

        StepVerifier
                .create(out)
                .assertNext(s -> {
                    assertThat(s.getValue().settings.setpoint).isEqualTo(setpoint);
                    assertThat(s.getValue().status.calling).isTrue();
                    assertThat(s.payload).isEqualTo(name);
                })
                .assertNext(s -> assertThat(s.getValue().status.calling).isFalse())
                .assertNext(s -> assertThat(s.getValue().status.calling).isFalse())
                .verifyComplete();
    }

    @Test
    void disabled() {

        var setpoint = 20.0;
        var signalOK = new Signal<Double, String>(Instant.now(), 30.0);
        var sequence = Flux.just(signalOK);
        var name = UUID.randomUUID().toString();
        var ts = new Thermostat(name, setpoint, 1, 0, 0, 1);
        var settings = new ZoneSettings(ts.getSetpoint());
        var z = new Zone(ts, settings);

        z.setSettings(new ZoneSettings(settings, false));

        var out = z
                .compute(sequence)
                .doOnNext(e -> logger.debug("zone/{}}: {}", name, e));

        // The thermostat is calling, but the zone has shut it off
        StepVerifier
                .create(out)
                .assertNext(s -> {
                    assertThat(s.getValue().settings.setpoint).isEqualTo(setpoint);
                    assertThat(s.getValue().status.calling).isFalse();
                    assertThat(s.payload).isEqualTo(name);
                })
                .verifyComplete();
    }
}
