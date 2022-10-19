package net.sf.dz3r.device.actuator;

import net.sf.dz3r.signal.Signal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingDeque;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class NullSwitchTest {

    protected final Logger logger = LogManager.getLogger();

    @Test
    void nullAddress() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new NullSwitch(null))
                .withMessage("address can't be null");
    }

    @Test
    void failOnStartWithBlock() {

        var ns = new NullSwitch("ns");
        var state = ns.getState();
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(state::block)
                .withCauseInstanceOf(IOException.class)
                .withMessageContaining("setStateSync() hasn't been called yet");
    }

    @Test
    void passSetStateWithBlock() {
        assertThat(new NullSwitch("T").setState(true).block()).isTrue();
        assertThat(new NullSwitch("F").setState(false).block()).isFalse();
    }

    @Test
    void passSetStateWithFlux() throws InterruptedException {
        var ns = new NullSwitch("ns");
        var stateFlux = ns.getFlux();

        var stateSignals = new LinkedBlockingDeque<Signal<Switch.State, String>>(4);

        new Thread(() -> {
            stateFlux
                    .doOnNext(stateSignals::add)
                    .subscribe(s -> {
                        logger.debug("stateFlux: {}", s);
                    });

        }).start();

        var actionFlux = Flux
                .just(true, false)
                .map(ns::setState)
                .map(Mono::block)
                .log();

        StepVerifier.create(actionFlux)
                .assertNext(s -> assertThat(s).isTrue())
                .assertNext(s -> assertThat(s).isFalse())
                .verifyComplete();

        if (true) {

            // VT: NOTE: The rest of this test makes Gradle hang
            return;
        }

        var s0 = stateSignals.take();
        var s1 = stateSignals.take();
        var s2 = stateSignals.take();
        var s3 = stateSignals.take();

        assertThat(s0.getValue().requested).isTrue();
        assertThat(s0.getValue().actual).isNull();

        assertThat(s1.getValue().requested).isTrue();
        assertThat(s1.getValue().actual).isTrue();

        assertThat(s2.getValue().requested).isFalse();
        assertThat(s2.getValue().actual).isTrue();

        assertThat(s3.getValue().requested).isFalse();
        assertThat(s3.getValue().actual).isFalse();
    }

    @Test
    void passSetStateWithBlockDelaySingleScheduler() {

        assertThatCode(() -> {
            delay(null);
        }).doesNotThrowAnyException();
    }

    @Test
    void passSetStateWithBlockDelayElasticScheduler() {

        assertThatCode(() -> {
            delay(Schedulers.newBoundedElastic(1, 10, "e-single"));
        }).doesNotThrowAnyException();
    }

    private void delay(Scheduler scheduler) {
        assertThat(new NullSwitch("D", 10, 50, scheduler).setState(true).block()).isTrue();
    }
}
