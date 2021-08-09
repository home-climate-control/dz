package net.sf.dz3r.model;

import net.sf.dz3.device.model.impl.ThermostatModel;
import net.sf.dz3r.controller.ProcessController;
import net.sf.dz3r.signal.Signal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import reactor.tools.agent.ReactorDebugAgent;

import java.time.Instant;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

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

    @Test
    void signal() {

        ThreadContext.push("signal");

        try {

            var s1 = new Signal<>(Instant.now(), 42.0, null);
            var s2 = new Signal<>(Instant.now(), -42.0, null, Signal.Status.FAILURE_PARTIAL, new TimeoutException("stale sensor"));
            var s3 = new Signal<>(Instant.now(), null, null, Signal.Status.FAILURE_TOTAL, new TimeoutException("sensor is gone"));

            var in = Flux.just(s1, s2, s3);

            in.doOnNext(s -> logger.info("sample: {}", s)).subscribe().dispose();

            var onlyFull = in.filter(Signal::isOK);
            var onlyValid = in.filter(Predicate.not(Signal::isError));
            var onlyError = in.filter(Signal::isError);

            StepVerifier.create(onlyFull)
                    .assertNext(s -> assertThat(s.getValue()).isEqualTo(42.0))
                    .verifyComplete();

            StepVerifier.create(onlyValid)
                    .assertNext(s -> assertThat(s.getValue()).isEqualTo(42.0))
                    .assertNext(s -> assertThat(s.getValue()).isEqualTo(-42.0))
                    .verifyComplete();

            StepVerifier.create(onlyError)
                    .assertNext(s -> assertThat(s.getError()).isInstanceOf(TimeoutException.class).hasMessage("sensor is gone"))
                    .verifyComplete();

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Make sure that the thermostat doesn't choke on a partially failed signal and properly propagates it
     * to the zone controller.
     */
    @Test
    void signalPartialFailure() throws InterruptedException {

        ThreadContext.push("signalPartialFailure");

        try {

            var ts = new Thermostat("ts", 20.0, 1.0, 0, 0, 0);
            var signalTotalFailure = new Signal<Double, Double>(Instant.now(), null, null, Signal.Status.FAILURE_TOTAL, new TimeoutException("sensor is gone"));
            var signalPartialFailure = new Signal<Double, Double>(Instant.now(), 42.0, null, Signal.Status.FAILURE_PARTIAL, new TimeoutException("stale sensor"));
            var in = Flux.just(signalTotalFailure, signalPartialFailure);

            var out = ts
                    .compute(in)
                    .doOnNext(s -> logger.info("sample: {}", s));

            // Partial failure is not really a failure, nobody must notice other than instrumentation

            StepVerifier
                    .create(out)
                    .assertNext(this::assertTotal)
                    .assertNext(this::assertPartial)
                    .verifyComplete();

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Make sure that the thermostat doesn't choke on an error signal and properly propagates it
     * to the zone controller.
     */
    @Test
    void signalTotalFailure() throws InterruptedException {

        ThreadContext.push("signalTotalFailure");

        try {

            var ts = new Thermostat("ts", 20.0, 1.0, 0, 0, 0);

            var signalTotalFailure = new Signal<Double, Double>(Instant.now(), null, null, Signal.Status.FAILURE_TOTAL, new TimeoutException("sensor is gone"));
            var signalPartialFailure = new Signal<Double, Double>(Instant.now(), 42.0, null, Signal.Status.FAILURE_PARTIAL, new TimeoutException("stale sensor"));

            // Total failure is reported down the control path - finally, someone will have to notice and take action
            var fluxTotalFailure = Flux.just(signalTotalFailure);

            var controlTotalFailure = ts
                    .compute(fluxTotalFailure)
                    .doOnNext(s -> logger.info("sample/total: {}", s));

            StepVerifier
                    .create(controlTotalFailure)
                    .assertNext(this::assertTotal)
                    .verifyComplete();

        } finally {
            ThreadContext.pop();
        }
    }

    private void assertTotal(Signal<ProcessController.Status<Double>, Double> s) {

        // This is the hysteresis controller setpoint, not PID
        assertThat(s.getValue().setpoint).isZero();

        // This [process control] error can't be calculated because the signal doesn't exist
        assertThat(s.getValue().error).isNull();

        // NaN is an indication of a hard error
        assertThat(s.getValue().signal).isNaN();

        // Total error made it all the way - this is intended
        assertThat(s.isOK()).isFalse();
        assertThat(s.isError()).isTrue();
    }

    private void assertPartial(Signal<ProcessController.Status<Double>, Double> s) {

        // This is the hysteresis controller setpoint, not PID
        assertThat(s.getValue().setpoint).isZero();

        // This [process control] error is common to both of them, the second will just pass it through
        assertThat(s.getValue().error).isEqualTo(22.0);

        // Yep, it is calling
        assertThat(s.getValue().signal).isEqualTo(1.0);

        // Partial [control path] error got masked by the PID controller, it's invisible here - this is expected
        assertThat(s.isOK()).isTrue();
        assertThat(s.isError()).isFalse();
    }
}
