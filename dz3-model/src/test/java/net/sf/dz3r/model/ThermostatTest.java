package net.sf.dz3r.model;

import net.sf.dz3.device.model.impl.ThermostatModel;
import net.sf.dz3r.signal.Signal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import reactor.tools.agent.ReactorDebugAgent;

import java.time.Instant;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for {@link ThermostatModel}.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2009-2021
 */
class ThermostatTest {

    private final Logger logger = LogManager.getLogger(getClass());

    @BeforeAll
    static void init() {
        ReactorDebugAgent.init();
    }

    /**
     * Make sure that the thermostat doesn't choke on an error signal and properly propagates it
     * to the zone controller.
     *
     */
    @Test
    void partialError() throws InterruptedException {

        var ts = new Thermostat("ts", 20.0, 1.0, 0, 0, 0);
        Flux<Signal<Double>> in = Flux.just(new Signal<Double>(Instant.now(), 42.0, Signal.Status.FAILURE_PARTIAL, new TimeoutException("stale sensor A")));

        StepVerifier
                .create(in)
                .assertNext(s -> {
                    assertThat(s.getStatus()).isEqualTo(Signal.Status.FAILURE_PARTIAL);
                    assertThat(s.getError()).isInstanceOf(TimeoutException.class).hasMessage("stale sensor A");
                })
                .verifyComplete();

        var out = ts
                .compute(in)
                .doOnNext(s -> logger.info("sample: {}", s));

        // Partial failure is not really a failure, nobody must notice other than instrumentation

        StepVerifier
                .create(out)
                .assertNext(s -> {

                    // This is the hysteresis controller setpoint, not PID
                    assertThat(s.getValue().setpoint).isZero();

                    // This error is common to both of them, the second will just pass it through
                    assertThat(s.getValue().error).isEqualTo(22.0);

                    // Yep, it is calling
                    assertThat(s.getValue().signal).isEqualTo(1.0);

                    // Partial error got masked by the PID controller, it's invisible here - this is expected
                    assertThat(s.isOK()).isTrue();
                    assertThat(s.isError()).isFalse();

                })
                .verifyComplete();
    }
}
