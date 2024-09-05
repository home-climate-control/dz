package net.sf.dz3r.device.actuator.economizer.v2;

import com.homeclimatecontrol.hcc.model.EconomizerSettings;
import net.sf.dz3r.device.DeviceState;
import net.sf.dz3r.device.actuator.NullCqrsSwitch;
import net.sf.dz3r.device.actuator.SwitchableHvacDevice;
import net.sf.dz3r.device.actuator.economizer.EconomizerConfig;
import net.sf.dz3r.model.HvacMode;
import com.homeclimatecontrol.hcc.signal.Signal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.LinkedBlockingQueue;
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
    @Test
    void ambientSensorLoss() throws InterruptedException {

        var actuator = new NullCqrsSwitch("test");
        var hvac = new SwitchableHvacDevice(
                Clock.systemUTC(),
                "test",
                HvacMode.COOLING,
                actuator,
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

        var deviceState = new LinkedBlockingQueue<Signal<DeviceState<Boolean>, String>>();

        actuator.getFlux().subscribe(deviceState::add);

        // Setup complete, let's push data now

        // After receiving just the indoor signal (but not ambient), the economizer is off
        indoorSink.tryEmitNext(new Signal<>(start, 25.0, "indoor"));
        assertThat(deviceState.take().getValue().requested).isFalse();

        // When both indoor and ambient signals are available, it is now on
        ambientSink.tryEmitNext(new Signal<>(start.plus(Duration.ofMillis(offset.addAndGet(timeStep))), 20.0));
        assertThat(deviceState.take().getValue().requested).isTrue();

        // It should turn off now
        ambientSink.tryEmitNext(new Signal<>(start.plus(Duration.ofMillis(offset.addAndGet(timeStep))), null, null,Signal.Status.FAILURE_TOTAL, new TimeoutException("oops")));

        // ...and it does.
        assertThat(deviceState.take().getValue().requested).isFalse();
    }
}
