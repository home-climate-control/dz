package net.sf.dz3r.model;

import net.sf.dz3r.signal.Signal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.concurrent.TimeoutException;

class ZoneTest {

    private final Logger logger = LogManager.getLogger();

    @Test
    void enabled() {

        var signalOK = new Signal<Double, Void>(Instant.now(), 42.0);
        var signalPartialFailure = new Signal<Double, Void>(Instant.now(), -42.0, null, Signal.Status.FAILURE_PARTIAL, new TimeoutException("stale sensor"));
        var signalTotalFailure = new Signal<Double, Void>(Instant.now(), null, null, Signal.Status.FAILURE_TOTAL, new TimeoutException("sensor is gone"));

        var sequence = Flux.just(
                signalOK,
                signalPartialFailure,
                signalTotalFailure
        );

        var ts = new Thermostat("ts", 42, 1, 0, 0, 1);
        var z = new Zone(ts);

        var flux = z
                .compute(sequence)
                .doOnNext(e -> logger.debug("zone: {}", e));

        flux.subscribe().dispose();
    }
}
