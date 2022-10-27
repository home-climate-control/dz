package net.sf.dz3r.device.actuator.economizer.v1;

import net.sf.dz3r.device.actuator.NullSwitch;
import net.sf.dz3r.device.actuator.economizer.EconomizerSettings;
import net.sf.dz3r.model.HvacMode;
import net.sf.dz3r.signal.Signal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.tools.agent.ReactorDebugAgent;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;

class SimpleEconomizerTest {

    private final Logger logger = LogManager.getLogger();

    @BeforeAll
    static void init() {
        ReactorDebugAgent.init();
    }

    /**
     * Make sure that the economizer turns on when the ambient temperature falls below its trigger point,
     * and then turns off when it raises again.
     */
    @Test
    void dropToOnAndBack() {

        // Target temperature is below the lowest in the ambient flux,
        // the economizer will just turn on and off
        var settings = new EconomizerSettings(
                HvacMode.COOLING,
                true,
                2,
                10);

        var indoor = 25.0;

        // VT: FIXME: Replace with a mock to verify()
        var targetDevice = new NullSwitch("s");

        // Can't feed it right away, it will all be consumed within the constructor
        var ambientFlux = getAmbientFlux();
        var sinkWrapper = new SinkWrapper<Signal<Double, Void>>();
        var deferredAmbientFlux = Flux.create(sinkWrapper::connect);

        var economizer = new SimpleEconomizer<String>(
                "economizer",
                settings,
                deferredAmbientFlux,
                targetDevice);

        economizer
                .compute(Flux.just(new Signal<>(Instant.now(), indoor)))
                .blockLast();

        var ambientSink = sinkWrapper.getSink();

        ambientFlux
                .doOnNext(ambientSink::next)
                .blockLast();
    }


    /**
     * Make sure that the economizer turns on when the ambient temperature falls below its trigger point,
     * then turns off when the indoor temperature falls below the target temperature,
     * and then turns on and then off when it raises again.
     */
    @Test
    @Disabled
    void dropToBelowTargetAndBack() {

        // Target temperature is within the range of the ambient flux,
        // the economizer will turn on, then off, then on and off again
        var settings = new EconomizerSettings(
                HvacMode.COOLING,
                true,
                2,
                18);

        var ambient = getAmbientFlux();
    }

    /**
     * Get a flux of values 1 apart from 30 to 15 and back to 30.
     */
    private Flux<Signal<Double, Void>> getAmbientFlux() {
        return Flux.concat(
                        Flux.range(0, 15).map(t -> 30 - t),
                        Flux.range(15, 16))
                .map(Integer::doubleValue)
                .doOnNext(t -> logger.info("emit ambient={}", t))
                .map(t  -> new Signal<>(Instant.now(), t));
    }

    private static class SinkWrapper<T> {
        private final Logger logger = LogManager.getLogger();
        private FluxSink<T> sink;
        private final CountDownLatch gate = new CountDownLatch(1);

        public void connect(FluxSink<T> sink) {
            this.sink = sink;
            gate.countDown();
            logger.info("sink is ready");
        }

        public FluxSink<T> getSink() {
            logger.info("acquiring sink...");
            try {
                gate.await();
                logger.info("sink acquired");
                return sink;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
