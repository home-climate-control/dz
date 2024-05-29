package net.sf.dz3r.device.actuator.economizer.v2;

import net.sf.dz3r.device.actuator.NullCqrsSwitch;
import net.sf.dz3r.device.actuator.SwitchableHvacDevice;
import net.sf.dz3r.device.actuator.economizer.EconomizerConfig;
import net.sf.dz3r.device.actuator.economizer.EconomizerSettings;
import net.sf.dz3r.model.HvacMode;
import net.sf.dz3r.signal.Signal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.RepeatedTest;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class PidEconomizerTest {

    private final Logger logger = LogManager.getLogger();

    /**
     * Make sure the economizer stops upon loss of the ambient sensor signal.
     *
     * See <a href="https://github.com/home-climate-control/dz/issues/320">#320</a>.
     */
    @RepeatedTest(value = 10, failureThreshold = 1)
    void ambientSensorLoss() throws InterruptedException {

        var s = new NullCqrsSwitch("test");
        var hvac = new SwitchableHvacDevice(
                Clock.systemUTC(),
                "test",
                HvacMode.COOLING,
                s,
                false,
                null
        );

        Sinks.Many<Signal<Double, Void>> ambientSink = Sinks.many().multicast().onBackpressureBuffer();
        Sinks.Many<Signal<Double, String>> indoorSink = Sinks.many().multicast().onBackpressureBuffer();

        var ambientFlux = ambientSink.asFlux();
        var indoorFlux = indoorSink.asFlux();

        var eco = new PidEconomizer<>(
                Clock.systemUTC(),
                "test",
                new EconomizerConfig(
                        HvacMode.COOLING,
                        1.0,
                        0.0000008,
                        1.1,
                        new EconomizerSettings(
                                0,
                                20,
                                true, 1.0
                        )

                ),
                ambientFlux,
                hvac,
                Duration.ofSeconds(10)
        );

        var output = eco.compute(indoorFlux);
        var start = Clock.systemUTC().instant();
        var timeStep = 10L;
        var offset = new AtomicLong(0);

        output
                .publishOn(Schedulers.boundedElastic())
                .subscribe(step -> logger.info("step: {}", step));

        // Setup complete, let's push data now

        // Initially, the economizer is on
        indoorSink.tryEmitNext(new Signal<>(start, 25.0, "indoor"));
        ambientSink.tryEmitNext(new Signal<>(start.plus(Duration.ofMillis(offset.addAndGet(timeStep))), 20.0));

        Thread.sleep(10); // NOSONAR Risks have been assessed and accepted
        assertThat(s.getState().requested).isTrue();

        // It should turn off now
        ambientSink.tryEmitNext(new Signal<>(start.plus(Duration.ofMillis(offset.addAndGet(timeStep))), null, null,Signal.Status.FAILURE_TOTAL, new TimeoutException("oops")));

        Thread.sleep(10); // NOSONAR Risks have been assessed and accepted

        // As of rev. 139c3ae65438f4a98c90e74dbffe24073e178d20:
        // If at any time the I component of stage 1 controller signal is more than 0, this makes its output less than the renderer threshold because the value of -1 is used for signaling,
        // and the economizer gets stuck on.
        assertThat(s.getState().requested).isFalse();
    }
}
