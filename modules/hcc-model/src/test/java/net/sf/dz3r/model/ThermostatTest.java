package net.sf.dz3r.model;

import net.sf.dz3r.controller.ProcessController;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.hvac.CallingStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.tools.agent.ReactorDebugAgent;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Test cases for {@link Thermostat}.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
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
    void signalPartialFailure() {

        ThreadContext.push("signalPartialFailure");

        try {

            var ts = new Thermostat("ts", 20.0, 1.0, 0, 0, 0);
            var signalOK = new Signal<Double, Void>(Instant.now(), 42.0);
            var signalTotalFailure = new Signal<Double, Void>(Instant.now(), null, null, Signal.Status.FAILURE_TOTAL, new TimeoutException("sensor is gone"));
            var signalPartialFailure = new Signal<Double, Void>(Instant.now(), 42.0, null, Signal.Status.FAILURE_PARTIAL, new TimeoutException("stale sensor"));
            var in = Flux.just(signalOK, signalTotalFailure, signalPartialFailure);

            var out = ts
                    .compute(in)
                    .doOnNext(s -> logger.info("sample: {}", s));

            // Partial failure is not really a failure, nobody must notice other than instrumentation

            StepVerifier
                    .create(out)
                    .assertNext(this::assertOK)
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
    void signalTotalFailure() {

        ThreadContext.push("signalTotalFailure");

        try {

            var ts = new Thermostat("ts", 20.0, 1.0, 0, 0, 0);

            var signalTotalFailure = new Signal<Double, Void>(Instant.now(), null, null, Signal.Status.FAILURE_TOTAL, new TimeoutException("sensor is gone"));

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

    /**
     * Make sure that the thermostat can be created with a {@code null} setpoint.
     */
    @Test
    void nullSetpointOnInstantiation() {

        var ts = new Thermostat("ts", null, 1.0, 0, 0, 0);

        logger.info("null setpoint: {}", ts.getSetpoint());
    }

    private void assertOK(Signal<ProcessController.Status<CallingStatus>, Void> s) {

        assertThat(s.getValue().setpoint).isEqualTo(20.0);
        assertThat(s.getValue().error).isEqualTo(22.0);
        assertThat(s.getValue().signal.calling).isTrue();

        // This signal is totally fine
        assertThat(s.isOK()).isTrue();
        assertThat(s.isError()).isFalse();
    }

    private void assertTotal(Signal<ProcessController.Status<CallingStatus>, Void> s) {

        assertThat(s.getValue().setpoint).isEqualTo(20.0);

        // Total error made it all the way - this is intended
        assertThat(s.isOK()).isFalse();
        assertThat(s.isError()).isTrue();

        // Special case: total failure, but the value still exists
        assertThat(s.getValue().error).isZero();
        assertThat(s.getValue().signal.demand).isZero();
    }

    private void assertPartial(Signal<ProcessController.Status<CallingStatus>, Void> s) {

        assertThat(s.getValue().setpoint).isEqualTo(20.0);
        assertThat(s.getValue().error).isEqualTo(22.0);
        assertThat(s.getValue().signal.calling).isTrue();

        // Partial [control path] error got masked by the PID controller, it's invisible here - this is expected
        assertThat(s.isOK()).isTrue();
        assertThat(s.isError()).isFalse();
    }

    @Test
    void setpointChangeEmitsSignal() {

        Sinks.Many<Double> pvSink = Sinks.many().multicast().onBackpressureBuffer();
        var source = pvSink
                .asFlux()
                .map(v -> new Signal<Double, Void>(Instant.now(), v));

        var ts = new Thermostat("ts", 20.0, 1, 0, 0, 1.1);

        var accumulator = new ArrayList<Signal<ProcessController.Status<CallingStatus>, Void>>();
        var out = ts
                .compute(source)
                .log()
                .subscribe(accumulator::add);

        pvSink.tryEmitNext(15.0);
        pvSink.tryEmitNext(25.0);

        ts.setSetpoint(30.0);

        pvSink.tryEmitNext(35.0);

        pvSink.tryEmitComplete();

        // Three signals corresponding to process variable change, and one to setpoint change
        assertThat(accumulator).hasSize(4);

        // PV change
        assertThat(accumulator.get(0).getValue().signal.calling).isFalse();
        assertThat(accumulator.get(1).getValue().signal.calling).isTrue();

        // Setpoint change
        assertThat(accumulator.get(2).getValue().signal.calling).isFalse();

        // PV change again
        assertThat(accumulator.get(3).getValue().signal.calling).isTrue();

        out.dispose();
    }

    /**
     * Make sure setpoint change causes a trail of adjusted signals according to its half-life - in cooling mode.
     */
    @Test
    void setpointChangeHalfLifeCooling() throws InterruptedException {
        assertThatCode(() -> {
            setpointChangeHalfLife(
                    new Thermostat(Clock.systemUTC(), "ts", new Range<>(10d, 40d), 25.0, -1.0, 0, 0, 0, Duration.ofSeconds(10), 1),
                    20d,
                    30d);
        })
                .doesNotThrowAnyException();
    }

    /**
     * Make sure setpoint change causes a trail of adjusted signals according to its half-life - in heating mode.
     */
    @Test
    void setpointChangeHalfLifeHeating() throws InterruptedException {
        assertThatCode(() -> {
            setpointChangeHalfLife(
                    new Thermostat(Clock.systemUTC(), "ts", new Range<>(10d, 40d), 25.0, 1.0, 0, 0, 0, Duration.ofSeconds(10), 1),
                    30d,
                    10d);
        })
                .doesNotThrowAnyException();
    }

    private void setpointChangeHalfLife(Thermostat ts, Double changedSetpoint, Double temperature) throws InterruptedException {
        var count = 20;
        var deltaT = 1;
        var offset = 0;
        var start = Instant.now();
        Sinks.Many<Signal<Double, Void>> sourceSink = Sinks.many().multicast().onBackpressureBuffer();
        var source = sourceSink.asFlux();

        var changeGate = new CountDownLatch(1);
        var stopGate = new CountDownLatch(1);

        ts
                .compute(source)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(ignored -> changeGate.countDown())
                .doOnComplete(stopGate::countDown)

                // The data structure passing the sensitivity controller output here is not yet in place, so let's just watch the logs until it is
                .subscribe(s -> logger.info("output: {}", s));

        // Prime the controller
        sourceSink.tryEmitNext(new Signal<Double, Void>(start, temperature));

        changeGate.await();

        // Introduce a change increasing the demand
        ts.setSetpoint(changedSetpoint);

        // Feed the rest
        while (offset++ < count) {
            sourceSink.tryEmitNext(new Signal<Double, Void>(start.plus(Duration.ofSeconds(offset * deltaT)), temperature));
        }

        sourceSink.tryEmitComplete();
        stopGate.await();
    }
}
